package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.waker.AbstractIntegrationTest;
import com.waker.common.AuthRateLimiter;
import com.waker.penalty.PenaltyEventLedger;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("penalty-dispatch-reliability-it")
@Import(PenaltyDispatchReliabilityIT.MutableClockConfig.class)
class PenaltyDispatchReliabilityIT extends AbstractIntegrationTest {

  private static final Instant FIXED_START = Instant.parse("2026-08-01T12:00:00Z");
  private static final String PASSWORD = "correct-horse-battery";
  private static final String CODE_PAYLOAD = "kitchen-fridge-2026";
  private static final GreenMail GREEN_MAIL = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

  static {
    GREEN_MAIL.start();
  }

  @AfterAll
  static void stopMail() {
    GREEN_MAIL.stop();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock mutableClock;
  @Autowired private CommitmentSweepJob commitmentSweepJob;
  @Autowired private JavaMailSenderImpl javaMailSender;
  @Autowired private PenaltyEventLedger penaltyEventLedger;

  @DynamicPropertySource
  static void registerMail(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.host", () -> "127.0.0.1");
    registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getServerSetup().getPort());
    registry.add("spring.mail.username", () -> "");
    registry.add("spring.mail.password", () -> "");
    registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
    registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    registry.add("waker.mail.from", () -> "waker@localhost");
  }

  @BeforeEach
  void resetState() throws Exception {
    authRateLimiter.reset();
    mutableClock.setInstant(FIXED_START);
    GREEN_MAIL.purgeEmailFromAllMailboxes();
    javaMailSender.setHost("127.0.0.1");
    javaMailSender.setPort(GREEN_MAIL.getSmtp().getServerSetup().getPort());
    // Append-only outbox forbids DELETE — terminalize leftovers from earlier ITs.
    jdbcTemplate.update("DELETE FROM leaderboard_entries");
    for (UUID eventId : penaltyEventLedger.findPendingEventIds(1_000)) {
      if (penaltyEventLedger.claim(eventId)) {
        penaltyEventLedger.markFailed(eventId);
      }
    }
  }

  @Test
  void pendingRecoveryDispatchesEmailExactlyOnceAcrossRestartAndSecondPass() throws Exception {
    String token = registerAndLogin("dispatch.email-once@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    // Crash gap: miss+PENDING committed, dispatch not yet run.
    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.runSweep();
    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
    assertThat(eventOutcome(commitmentId)).isEqualTo("PENDING");
    assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();

    // Recovery pass — must actually deliver (count == 1, not merely <= 1).
    commitmentSweepJob.runDispatch();
    assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
    MimeMessage[] afterFirst = GREEN_MAIL.getReceivedMessages();
    assertThat(afterFirst).hasSize(1);
    assertThat(afterFirst[0].getAllRecipients()[0].toString()).contains("friend@example.com");
    assertThat(eventOutcome(commitmentId)).isEqualTo("DISPATCHED");

    // Second pass must not re-dispatch.
    commitmentSweepJob.runDispatch();
    assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1);
    assertThat(eventOutcome(commitmentId)).isEqualTo("DISPATCHED");
  }

  @Test
  void leaderboardHappyPathThroughProductionDispatcherIsExactlyOnce() throws Exception {
    String token = registerAndLogin("dispatch.leaderboard-once@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "LEADERBOARD");

    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.runSweep();
    assertThat(eventOutcome(commitmentId)).isEqualTo("PENDING");
    assertThat(leaderboardCount(commitmentId)).isZero();

    commitmentSweepJob.runDispatch();
    assertThat(leaderboardCount(commitmentId)).isEqualTo(1);
    assertThat(eventOutcome(commitmentId)).isEqualTo("DISPATCHED");

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT user_display_name, commitment_name
            FROM leaderboard_entries
            WHERE commitment_id = ?::uuid
            """,
            commitmentId);
    assertThat(row.get("user_display_name")).isEqualTo("Dispatch Tester");
    assertThat(row.get("commitment_name")).isEqualTo("Dispatch commitment");

    commitmentSweepJob.runDispatch();
    assertThat(leaderboardCount(commitmentId)).isEqualTo(1);
  }

  @Test
  void concurrentDispatchersInvokeHandlerSideEffectExactlyOnce() throws Exception {
    String token = registerAndLogin("dispatch.concurrent@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.runSweep();
    assertThat(eventOutcome(commitmentId)).isEqualTo("PENDING");

    int threads = 4;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger runs = new AtomicInteger();
    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  commitmentSweepJob.runDispatch();
                  runs.incrementAndGet();
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(30, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(runs.get()).isEqualTo(threads);
    assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
    MimeMessage[] messages = GREEN_MAIL.getReceivedMessages();
    assertThat(messages).hasSize(1);
    assertThat(messages[0].getSubject()).contains("Dispatch commitment");
    assertThat(eventOutcome(commitmentId)).isEqualTo("DISPATCHED");
    assertThat(pendingEventCount()).isZero();
  }

  @Test
  void handlerFailureMarksFailedAndIsNotRetried() throws Exception {
    String token = registerAndLogin("dispatch.fail@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.runSweep();

    int savedPort = javaMailSender.getPort();
    javaMailSender.setHost("127.0.0.1");
    javaMailSender.setPort(1);
    try {
      commitmentSweepJob.runDispatch();
      assertThat(eventOutcome(commitmentId)).isEqualTo("FAILED");
      assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();

      // Later pass must not auto-retry terminal FAILED.
      commitmentSweepJob.runDispatch();
      assertThat(eventOutcome(commitmentId)).isEqualTo("FAILED");
      assertThat(GREEN_MAIL.getReceivedMessages()).isEmpty();
    } finally {
      javaMailSender.setHost("127.0.0.1");
      javaMailSender.setPort(savedPort);
    }
  }

  @Test
  void scheduledCycleRunsMissThenDispatch() throws Exception {
    String token = registerAndLogin("dispatch.cycle@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.scheduledCycle();

    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
    assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
    assertThat(GREEN_MAIL.getReceivedMessages()).hasSize(1);
    assertThat(eventOutcome(commitmentId)).isEqualTo("DISPATCHED");
  }

  private int leaderboardCount(String commitmentId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM leaderboard_entries WHERE commitment_id = ?::uuid",
            Integer.class,
            commitmentId);
    return count == null ? 0 : count;
  }

  private int pendingEventCount() {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM penalty_events WHERE outcome = 'PENDING'", Integer.class);
    return count == null ? 0 : count;
  }

  private String eventOutcome(String commitmentId) {
    return jdbcTemplate.queryForObject(
        "SELECT outcome FROM penalty_events WHERE commitment_id = ?::uuid",
        String.class,
        commitmentId);
  }

  private String commitmentStatus(String commitmentId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM commitments WHERE id = ?::uuid", String.class, commitmentId);
  }

  private String createCommitment(
      String token, Instant notifyTime, Instant deadline, String penaltyClass) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody(notifyTime, deadline, penaltyClass)))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private String registerAndLogin(String email) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email)))
        .andExpect(status().isCreated());

    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email)))
            .andExpect(status().isOk())
            .andReturn();

    return objectMapper
        .readTree(login.getResponse().getContentAsString())
        .get("accessToken")
        .asText();
  }

  private static String registerBody(String email) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "Dispatch",
          "lastName": "Tester"
        }
        """
        .formatted(email, PASSWORD);
  }

  private static String loginBody(String email) {
    return """
        {
          "email": "%s",
          "password": "%s"
        }
        """
        .formatted(email, PASSWORD);
  }

  private static String createBody(Instant notifyTime, Instant deadline, String penaltyClass) {
    String penaltyConfig =
        switch (penaltyClass) {
          case "LEADERBOARD" ->
              """
              { "_class": "LEADERBOARD", "consent": true }
              """;
          default ->
              """
              {
                "_class": "EMAIL_TO_CONTACT",
                "contactEmail": "friend@example.com",
                "message": "I missed my commitment."
              }
              """;
        };

    return """
        {
          "name": "Dispatch commitment",
          "description": "Dispatch reliability IT",
          "notifyTime": "%s",
          "deadline": "%s",
          "missionConfig": { "_class": "QR_CODE", "codePayload": "%s" },
          "penaltyConfig": %s
        }
        """
        .formatted(
            notifyTime.atOffset(ZoneOffset.UTC),
            deadline.atOffset(ZoneOffset.UTC),
            CODE_PAYLOAD,
            penaltyConfig);
  }

  static final class MutableClock extends Clock {

    private volatile Instant instant;
    private final ZoneId zone;

    MutableClock(Instant initial, ZoneId zone) {
      this.instant = initial;
      this.zone = zone;
    }

    void setInstant(Instant instant) {
      this.instant = instant;
    }

    void advance(Duration duration) {
      this.instant = this.instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return new MutableClock(instant, zone);
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }

  @TestConfiguration
  @Profile("penalty-dispatch-reliability-it")
  static class MutableClockConfig {

    @Bean("commitmentClock")
    MutableClock commitmentClock() {
      return new MutableClock(FIXED_START, ZoneOffset.UTC);
    }
  }
}

package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import com.waker.common.AuthRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("sweep-health-it")
@Import(SweepHealthIT.MutableClockConfig.class)
class SweepHealthIT extends AbstractIntegrationTest {

  private static final Instant FIXED_START = Instant.parse("2026-08-01T12:00:00Z");
  private static final String PASSWORD = "correct-horse-battery";
  private static final String CODE_PAYLOAD = "kitchen-fridge-2026";
  private static final int DOGFOOD_VOLUME = 75;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock mutableClock;
  @Autowired private CommitmentSweepJob commitmentSweepJob;
  @Autowired private SweepHealthState sweepHealthState;

  @DynamicPropertySource
  static void enlargeDispatchBatch(DynamicPropertyRegistry registry) {
    registry.add("waker.sweep.dispatch-batch-size", () -> String.valueOf(DOGFOOD_VOLUME));
  }

  @BeforeEach
  void resetState() {
    authRateLimiter.reset();
    mutableClock.setInstant(FIXED_START);
    sweepHealthState.clearForTests();
  }

  @Test
  void actuatorHealthExposesNeverBeforeFirstSuccessfulSweep() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.sweep.status").value("UP"))
        .andExpect(jsonPath("$.components.sweep.details.lastSuccessfulSweepRun").value("never"));
  }

  @Test
  void successfulSweepAdvancesLastSuccessfulSweepRunInActuator() throws Exception {
    String token = registerAndLogin("sweep.health@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    createCommitment(token, notifyTime, deadline, "LEADERBOARD");

    Instant before = Instant.now(mutableClock);
    assertThat(sweepHealthState.lastSuccessfulRun()).isEmpty();

    mutableClock.advance(Duration.ofMinutes(31));
    Instant expectedRunAt = Instant.now(mutableClock);
    commitmentSweepJob.scheduledCycle();

    assertThat(sweepHealthState.lastSuccessfulRun()).contains(expectedRunAt);
    assertThat(expectedRunAt).isAfter(before);

    MvcResult health =
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn();
    JsonNode sweep =
        objectMapper
            .readTree(health.getResponse().getContentAsString())
            .path("components")
            .path("sweep");
    assertThat(sweep.path("status").asText()).isEqualTo("UP");
    Instant reported = Instant.parse(sweep.path("details").path("lastSuccessfulSweepRun").asText());
    assertThat(reported).isEqualTo(expectedRunAt);
  }

  @Test
  void dogfoodScaleSweepAndDispatchCompletesWithinSixtySeconds() throws Exception {
    String token = registerAndLogin("sweep.nfr1@example.com");
    UUID userId = userIdForEmail("sweep.nfr1@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    seedOverduePendingCommitments(userId, notifyTime, deadline, DOGFOOD_VOLUME);

    // Warm auth path already done; volume is JDBC-seeded.
    assertThat(token).isNotBlank();

    mutableClock.advance(Duration.ofMinutes(31));
    long started = System.nanoTime();
    commitmentSweepJob.scheduledCycle();
    long elapsedMs = Duration.ofNanos(System.nanoTime() - started).toMillis();

    assertThat(elapsedMs).isLessThan(60_000);
    assertThat(sweepHealthState.lastSuccessfulRun()).isPresent();
    Integer missed =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM commitments WHERE user_id = ?::uuid AND status = 'MISSED'",
            Integer.class,
            userId.toString());
    assertThat(missed).isEqualTo(DOGFOOD_VOLUME);
  }

  private void seedOverduePendingCommitments(
      UUID userId, Instant notifyTime, Instant deadline, int count) {
    Instant now = FIXED_START;
    for (int i = 0; i < count; i++) {
      UUID commitmentId = UUID.randomUUID();
      jdbcTemplate.update(
          """
          INSERT INTO commitments (
              id, user_id, name, description, status, notify_time, deadline,
              mission_config, penalty_config, created_at, updated_at)
          VALUES (
              ?::uuid, ?::uuid, ?, NULL, 'PENDING',
              ?::timestamptz, ?::timestamptz,
              '{"_class":"QR_CODE","codePayload":"fixture"}'::jsonb,
              '{"_class":"LEADERBOARD","consent":true}'::jsonb,
              ?::timestamptz, ?::timestamptz)
          """,
          commitmentId.toString(),
          userId.toString(),
          "NFR1 commitment " + i,
          notifyTime.toString(),
          deadline.toString(),
          now.toString(),
          now.toString());
    }
  }

  private UUID userIdForEmail(String email) {
    return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
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
          "firstName": "Sweep",
          "lastName": "Health"
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
          "name": "Sweep health commitment",
          "description": "SweepHealthIT",
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
  @Profile("sweep-health-it")
  static class MutableClockConfig {

    @Bean("commitmentClock")
    MutableClock commitmentClock() {
      return new MutableClock(FIXED_START, ZoneOffset.UTC);
    }
  }
}

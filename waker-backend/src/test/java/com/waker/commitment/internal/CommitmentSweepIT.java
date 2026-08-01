package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import com.waker.common.AuthRateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("commitment-sweep-it")
@Import(CommitmentSweepIT.MutableClockConfig.class)
class CommitmentSweepIT extends AbstractIntegrationTest {

  private static final Instant FIXED_START = Instant.parse("2026-08-01T12:00:00Z");
  private static final String PASSWORD = "correct-horse-battery";
  private static final String CODE_PAYLOAD = "kitchen-fridge-2026";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock mutableClock;
  @Autowired private CommitmentSweepJob commitmentSweepJob;
  @Autowired private CommitmentMissEnforcer missEnforcer;

  @BeforeEach
  void resetState() {
    authRateLimiter.reset();
    mutableClock.setInstant(FIXED_START);
  }

  @Test
  void clockAdvanceMarksOverduePendingMissedAndEnqueuesPenaltyEvent() throws Exception {
    String token = registerAndLogin("sweep.happy@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(31));
    commitmentSweepJob.runSweep();

    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
    List<Map<String, Object>> events = penaltyEventsFor(commitmentId);
    assertThat(events).hasSize(1);
    assertThat(events.getFirst().get("outcome")).isEqualTo("PENDING");
    assertThat(events.getFirst().get("penalty_type")).isEqualTo("EMAIL_TO_CONTACT");
    assertThat(events.getFirst().get("commitment_id")).isEqualTo(commitmentId);
  }

  @Test
  void notYetOverdueIsLeftPendingWithoutPenaltyEvent() throws Exception {
    String token = registerAndLogin("sweep.notyet@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(29));
    commitmentSweepJob.runSweep();

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
    assertThat(penaltyEventsFor(commitmentId)).isEmpty();
  }

  @Test
  void fulfillWinsRaceLeavesFulfilledWithoutPenaltyEvent() throws Exception {
    String token = registerAndLogin("sweep.fulfill-race@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    mutableClock.advance(Duration.ofMinutes(31));

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody()))
        .andExpect(status().isOk());

    commitmentSweepJob.runSweep();

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
    assertThat(penaltyEventsFor(commitmentId)).isEmpty();
  }

  @Test
  void cancelledAndFulfilledRowsAreNeverSelectedEvenIfDeadlinePassed() throws Exception {
    String token = registerAndLogin("sweep.filtered@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);

    String cancelledId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");
    String fulfilledId =
        createCommitment(
            token,
            notifyTime.plus(1, ChronoUnit.MINUTES),
            deadline.plus(1, ChronoUnit.MINUTES),
            "LEADERBOARD");

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'CANCELLED' WHERE id = ?::uuid", cancelledId);
    jdbcTemplate.update(
        "UPDATE commitments SET status = 'FULFILLED' WHERE id = ?::uuid", fulfilledId);

    mutableClock.advance(Duration.ofHours(2));
    commitmentSweepJob.runSweep();

    assertThat(commitmentStatus(cancelledId)).isEqualTo("CANCELLED");
    assertThat(commitmentStatus(fulfilledId)).isEqualTo("FULFILLED");
    assertThat(penaltyEventsFor(cancelledId)).isEmpty();
    assertThat(penaltyEventsFor(fulfilledId)).isEmpty();
  }

  @Test
  void markMissedSkipPathWhenAlreadyNonPendingDoesNotInsertEvent() throws Exception {
    String token = registerAndLogin("sweep.skip@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);
    String commitmentId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'FULFILLED' WHERE id = ?::uuid", commitmentId);

    Instant now = Instant.now(mutableClock);
    missEnforcer.markMissedAndEnqueue(java.util.UUID.fromString(commitmentId), now);

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
    assertThat(penaltyEventsFor(commitmentId)).isEmpty();
  }

  @Test
  void twoOverdueCommitmentsAreBothMissedInOnePass() throws Exception {
    String token = registerAndLogin("sweep.multi@example.com");
    Instant notifyTime = FIXED_START.plus(15, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(30, ChronoUnit.MINUTES);

    String firstId = createCommitment(token, notifyTime, deadline, "EMAIL_TO_CONTACT");
    String secondId =
        createCommitment(
            token,
            notifyTime.plus(1, ChronoUnit.MINUTES),
            deadline.plus(1, ChronoUnit.MINUTES),
            "LEADERBOARD");

    mutableClock.advance(Duration.ofMinutes(32));
    commitmentSweepJob.runSweep();

    assertThat(commitmentStatus(firstId)).isEqualTo("MISSED");
    assertThat(commitmentStatus(secondId)).isEqualTo("MISSED");

    List<Map<String, Object>> firstEvents = penaltyEventsFor(firstId);
    List<Map<String, Object>> secondEvents = penaltyEventsFor(secondId);
    assertThat(firstEvents).hasSize(1);
    assertThat(secondEvents).hasSize(1);
    assertThat(firstEvents.getFirst().get("outcome")).isEqualTo("PENDING");
    assertThat(secondEvents.getFirst().get("outcome")).isEqualTo("PENDING");
    assertThat(firstEvents.getFirst().get("penalty_type")).isEqualTo("EMAIL_TO_CONTACT");
    assertThat(secondEvents.getFirst().get("penalty_type")).isEqualTo("LEADERBOARD");
  }

  private List<Map<String, Object>> penaltyEventsFor(String commitmentId) {
    return jdbcTemplate.queryForList(
        """
        SELECT commitment_id::text AS commitment_id, penalty_type, outcome
        FROM penalty_events
        WHERE commitment_id = ?::uuid
        """,
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
          "firstName": "Sweep",
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
          "name": "Sweep commitment",
          "description": "Sweep IT",
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

  private static String qrFulfillmentBody() {
    return """
        {
          "_class": "QR_CODE",
          "scannedPayload": "%s"
        }
        """
        .formatted(CODE_PAYLOAD);
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
  @Profile("commitment-sweep-it")
  static class MutableClockConfig {

    @Bean("commitmentClock")
    MutableClock commitmentClock() {
      return new MutableClock(FIXED_START, ZoneOffset.UTC);
    }
  }
}

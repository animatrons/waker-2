package com.waker.commitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("commitment-edit-it")
@Import(CommitmentEditCancelIT.MutableClockConfig.class)
class CommitmentEditCancelIT extends AbstractIntegrationTest {

  private static final Instant FIXED_START = Instant.parse("2026-07-30T12:00:00Z");
  private static final String PASSWORD = "correct-horse-battery";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MutableClock mutableClock;

  @BeforeEach
  void resetState() {
    authRateLimiter.reset();
    mutableClock.setInstant(FIXED_START);
  }

  @Test
  void editInsideEditWindowSucceeds() throws Exception {
    String token = registerAndLogin("edit.inside@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    updateBody("Updated workout", notifyTime, deadline, "kitchen-fridge-updated")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated workout"))
        .andExpect(jsonPath("$.status").value("PENDING"));

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT name FROM commitments WHERE id = ?::uuid", String.class, commitmentId))
        .isEqualTo("Updated workout");
  }

  @Test
  void editAfterEditWindowClosedReturns409() throws Exception {
    String token = registerAndLogin("edit.closed@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mutableClock.advance(Duration.ofMinutes(11));

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Too late", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void cancelInsideEditWindowReturns204AndCancelledStatus() throws Exception {
    String token = registerAndLogin("cancel.inside@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mockMvc
        .perform(
            delete("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("CANCELLED");
  }

  @Test
  void cancelAfterEditWindowClosedReturns409() throws Exception {
    String token = registerAndLogin("cancel.closed@example.com");
    Instant notifyTime = FIXED_START.plus(6, ChronoUnit.MINUTES);
    Instant deadline = FIXED_START.plus(2, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mutableClock.advance(Duration.ofMinutes(7));

    mockMvc
        .perform(
            delete("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void editDeadlineBeyondTwentyFourHoursFromCreationReturns400() throws Exception {
    String token = registerAndLogin("edit.horizon@example.com");
    Instant notifyTime = FIXED_START.plus(1, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(2, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    updateBody(
                        "Morning workout",
                        notifyTime,
                        FIXED_START.plus(25, ChronoUnit.HOURS),
                        "kitchen-fridge-2026")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void editOnCancelledCommitmentReturns409() throws Exception {
    String token = registerAndLogin("edit.cancelled@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);

    mockMvc
        .perform(
            delete("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Retry edit", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isConflict());
  }

  @Test
  void editOnFulfilledCommitmentReturns409() throws Exception {
    String token = registerAndLogin("edit.fulfilled@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(token, notifyTime, deadline);
    jdbcTemplate.update(
        "UPDATE commitments SET status = 'FULFILLED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Retry edit", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isConflict());
  }

  @Test
  void wrongOwnerReturns404() throws Exception {
    String ownerToken = registerAndLogin("edit.owner@example.com");
    String otherToken = registerAndLogin("edit.other@example.com");
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    String commitmentId = createCommitment(ownerToken, notifyTime, deadline);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Stolen edit", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            delete("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isNotFound());
  }

  @Test
  void unknownCommitmentReturns404() throws Exception {
    String token = registerAndLogin("edit.unknown@example.com");
    UUID unknownId = UUID.randomUUID();
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + unknownId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("Missing", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isNotFound());
  }

  @Test
  void unauthenticatedEditAndCancelReturn401() throws Exception {
    UUID id = UUID.randomUUID();
    Instant notifyTime = FIXED_START.plus(2, ChronoUnit.HOURS);
    Instant deadline = FIXED_START.plus(3, ChronoUnit.HOURS);

    mockMvc
        .perform(
            put("/api/v1/commitments/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody("No auth", notifyTime, deadline, "kitchen-fridge-2026")))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(delete("/api/v1/commitments/" + id)).andExpect(status().isUnauthorized());
  }

  private String createCommitment(String token, Instant notifyTime, Instant deadline)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody(notifyTime, deadline)))
            .andExpect(status().isCreated())
            .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  private String commitmentStatus(String commitmentId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM commitments WHERE id = ?::uuid", String.class, commitmentId);
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
          "firstName": "Edit",
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

  private static String createBody(Instant notifyTime, Instant deadline) {
    return """
        {
          "name": "Morning workout",
          "description": "Optional note",
          "notifyTime": "%s",
          "deadline": "%s",
          "missionConfig": { "_class": "QR_CODE", "codePayload": "kitchen-fridge-2026" },
          "penaltyConfig": {
            "_class": "EMAIL_TO_CONTACT",
            "contactEmail": "friend@example.com",
            "message": "I missed my commitment."
          }
        }
        """
        .formatted(notifyTime.atOffset(ZoneOffset.UTC), deadline.atOffset(ZoneOffset.UTC));
  }

  private static String updateBody(
      String name, Instant notifyTime, Instant deadline, String codePayload) {
    return """
        {
          "name": "%s",
          "description": "Updated note",
          "notifyTime": "%s",
          "deadline": "%s",
          "missionConfig": { "_class": "QR_CODE", "codePayload": "%s" },
          "penaltyConfig": {
            "_class": "EMAIL_TO_CONTACT",
            "contactEmail": "friend@example.com",
            "message": "I missed my commitment."
          }
        }
        """
        .formatted(
            name,
            notifyTime.atOffset(ZoneOffset.UTC),
            deadline.atOffset(ZoneOffset.UTC),
            codePayload);
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
  @Profile("commitment-edit-it")
  static class MutableClockConfig {

    @Bean("commitmentClock")
    MutableClock commitmentClock() {
      return new MutableClock(FIXED_START, ZoneOffset.UTC);
    }
  }
}

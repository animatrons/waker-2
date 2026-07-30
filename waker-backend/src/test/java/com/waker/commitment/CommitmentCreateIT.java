package com.waker.commitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import com.waker.common.AuthRateLimiter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CommitmentCreateIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetRateLimits() {
    authRateLimiter.reset();
  }

  @Test
  void createsCommitmentWithQrMissionAndEmailPenalty() throws Exception {
    String token = registerAndLogin("commit.create@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        validCreateBody(
                            now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.name").value("Morning workout"))
            .andExpect(jsonPath("$.missionConfig._class").value("QR_CODE"))
            .andExpect(jsonPath("$.penaltyConfig._class").value("EMAIL_TO_CONTACT"))
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM commitments WHERE id = ?::uuid AND status = 'PENDING'",
                Integer.class,
                body.get("id").asText()))
        .isEqualTo(1);
  }

  @Test
  void mathGameCreateGeneratesServerSideProblem() throws Exception {
    String token = registerAndLogin("commit.math@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mathGameCreateBody(
                        now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.missionConfig._class").value("MATH_GAME"))
        .andExpect(jsonPath("$.missionConfig.problemStatement").isNotEmpty())
        .andExpect(jsonPath("$.missionConfig.expectedAnswer").isNotEmpty());
  }

  @Test
  void rejectsNotifyTimeLessThanFiveMinutesAhead() throws Exception {
    String token = registerAndLogin("commit.timing1@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validCreateBody(
                        now.plus(2, ChronoUnit.MINUTES), now.plus(2, ChronoUnit.HOURS))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void rejectsNotifyTimeAfterTwentyFourHours() throws Exception {
    String token = registerAndLogin("commit.timing2@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validCreateBody(
                        now.plus(25, ChronoUnit.HOURS), now.plus(26, ChronoUnit.HOURS))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsNotifyTimeNotBeforeDeadline() throws Exception {
    String token = registerAndLogin("commit.timing3@example.com");
    Instant same = Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(2, ChronoUnit.HOURS);

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody(same, same)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsOffsetLessTimestamp() throws Exception {
    String token = registerAndLogin("commit.timing5@example.com");

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Morning workout",
                      "notifyTime": "2026-07-30T13:00:00",
                      "deadline": "2026-07-30T14:00:00",
                      "missionConfig": { "_class": "QR_CODE", "codePayload": "payload" },
                      "penaltyConfig": {
                        "_class": "EMAIL_TO_CONTACT",
                        "contactEmail": "friend@example.com",
                        "message": "Missed it."
                      }
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void sixthPendingCommitmentReturns409() throws Exception {
    String token = registerAndLogin("commit.cap@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    for (int i = 0; i < 5; i++) {
      mockMvc
          .perform(
              post("/api/v1/commitments")
                  .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      validCreateBody(
                          now.plus(60 + i, ChronoUnit.MINUTES),
                          now.plus(90 + i, ChronoUnit.MINUTES))))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validCreateBody(
                        now.plus(120, ChronoUnit.MINUTES), now.plus(150, ChronoUnit.MINUTES))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void unauthenticatedCreateReturns401() throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    mockMvc
        .perform(
            post("/api/v1/commitments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    validCreateBody(now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rejectsInvalidPenaltyConfig() throws Exception {
    String token = registerAndLogin("commit.penalty@example.com");

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Morning workout",
                      "notifyTime": "2026-07-30T13:00:00Z",
                      "deadline": "2026-07-30T14:00:00Z",
                      "missionConfig": { "_class": "QR_CODE", "codePayload": "payload" },
                      "penaltyConfig": { "_class": "LEADERBOARD", "consent": false }
                    }
                    """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void rejectsInvalidMissionConfig() throws Exception {
    String token = registerAndLogin("commit.mission@example.com");

    mockMvc
        .perform(
            post("/api/v1/commitments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Morning workout",
                      "notifyTime": "2026-07-30T13:00:00Z",
                      "deadline": "2026-07-30T14:00:00Z",
                      "missionConfig": { "_class": "QR_CODE", "codePayload": "   " },
                      "penaltyConfig": {
                        "_class": "EMAIL_TO_CONTACT",
                        "contactEmail": "friend@example.com",
                        "message": "Missed it."
                      }
                    }
                    """))
        .andExpect(status().isBadRequest());
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
          "firstName": "Commit",
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

  private static String validCreateBody(Instant notifyTime, Instant deadline) {
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

  private static String mathGameCreateBody(Instant notifyTime, Instant deadline) {
    return """
        {
          "name": "Math wake",
          "notifyTime": "%s",
          "deadline": "%s",
          "missionConfig": { "_class": "MATH_GAME" },
          "penaltyConfig": {
            "_class": "LEADERBOARD",
            "consent": true
          }
        }
        """
        .formatted(notifyTime.atOffset(ZoneOffset.UTC), deadline.atOffset(ZoneOffset.UTC));
  }
}

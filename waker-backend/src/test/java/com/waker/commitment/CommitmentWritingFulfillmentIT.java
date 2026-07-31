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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CommitmentWritingFulfillmentIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";
  private static final int MINIMUM_LENGTH = 10;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetRateLimits() {
    authRateLimiter.reset();
  }

  @Test
  void validTextFulfillsCommitment() throws Exception {
    String token = registerAndLogin("writing.happy@example.com");
    String commitmentId = createWritingCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(writingFulfillmentBody("1234567890")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLED"));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
  }

  @Test
  void textBelowMinimumReturns400AndStaysPending() throws Exception {
    String token = registerAndLogin("writing.short@example.com");
    String commitmentId = createWritingCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(writingFulfillmentBody("123456789")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void emptyTextReturns400() throws Exception {
    String token = registerAndLogin("writing.empty@example.com");
    String commitmentId = createWritingCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "_class": "WRITING_TASK",
                      "submittedText": "   "
                    }
                    """))
        .andExpect(status().isBadRequest());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void textAboveMaximumReturns400() throws Exception {
    String token = registerAndLogin("writing.long@example.com");
    String commitmentId = createWritingCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(writingFulfillmentBody("x".repeat(10_001))))
        .andExpect(status().isBadRequest());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void missedCommitmentReturns409() throws Exception {
    String token = registerAndLogin("writing.missed@example.com");
    String commitmentId = createWritingCommitment(token);

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'MISSED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(writingFulfillmentBody("1234567890")))
        .andExpect(status().isConflict());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
  }

  private String createWritingCommitment(String token) throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        writingCreateBody(
                            now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS))))
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

  private static String writingFulfillmentBody(String submittedText) {
    return """
        {
          "_class": "WRITING_TASK",
          "submittedText": "%s"
        }
        """
        .formatted(submittedText.replace("\\", "\\\\").replace("\"", "\\\""));
  }

  private static String registerBody(String email) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "Writing",
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

  private static String writingCreateBody(Instant notifyTime, Instant deadline) {
    return """
        {
          "name": "Morning writing",
          "notifyTime": "%s",
          "deadline": "%s",
          "missionConfig": {
            "_class": "WRITING_TASK",
            "prompt": "Write why you will wake up on time",
            "minimumLength": %d
          },
          "penaltyConfig": {
            "_class": "LEADERBOARD",
            "consent": true
          }
        }
        """
        .formatted(
            notifyTime.atOffset(ZoneOffset.UTC), deadline.atOffset(ZoneOffset.UTC), MINIMUM_LENGTH);
  }
}

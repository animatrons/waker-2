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
class CommitmentMathFulfillmentIT extends AbstractIntegrationTest {

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
  void createGeneratesServerSideProblemAndAnswer() throws Exception {
    String token = registerAndLogin("math.create@example.com");
    JsonNode missionConfig = createMathCommitment(token);

    assertThat(missionConfig.get("problemStatement").asText()).isNotBlank();
    assertThat(missionConfig.get("expectedAnswer").asText()).isNotBlank();
  }

  @Test
  void correctAnswerFulfillsCommitment() throws Exception {
    String token = registerAndLogin("math.happy@example.com");
    JsonNode missionConfig = createMathCommitment(token);
    String commitmentId = missionConfig.get("commitmentId").asText();
    String expectedAnswer = missionConfig.get("expectedAnswer").asText();

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mathFulfillmentBody(expectedAnswer)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLED"));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
  }

  @Test
  void whitespacePaddedCorrectAnswerFulfillsCommitment() throws Exception {
    String token = registerAndLogin("math.trim@example.com");
    JsonNode missionConfig = createMathCommitment(token);
    String commitmentId = missionConfig.get("commitmentId").asText();
    String expectedAnswer = missionConfig.get("expectedAnswer").asText();

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mathFulfillmentBody("  " + expectedAnswer + "  ")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLED"));
  }

  @Test
  void wrongAnswerReturns400AndStaysPending() throws Exception {
    String token = registerAndLogin("math.wrong@example.com");
    JsonNode missionConfig = createMathCommitment(token);
    String commitmentId = missionConfig.get("commitmentId").asText();

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mathFulfillmentBody("99999")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void missedCommitmentReturns409() throws Exception {
    String token = registerAndLogin("math.missed@example.com");
    JsonNode missionConfig = createMathCommitment(token);
    String commitmentId = missionConfig.get("commitmentId").asText();
    String expectedAnswer = missionConfig.get("expectedAnswer").asText();

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'MISSED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mathFulfillmentBody(expectedAnswer)))
        .andExpect(status().isConflict());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
  }

  private JsonNode createMathCommitment(String token) throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mathCreateBody(
                            now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.missionConfig._class").value("MATH_GAME"))
            .andExpect(jsonPath("$.missionConfig.problemStatement").isNotEmpty())
            .andExpect(jsonPath("$.missionConfig.expectedAnswer").isNotEmpty())
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode missionConfig = body.get("missionConfig");
    return objectMapper
        .createObjectNode()
        .put("commitmentId", body.get("id").asText())
        .put("problemStatement", missionConfig.get("problemStatement").asText())
        .put("expectedAnswer", missionConfig.get("expectedAnswer").asText());
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

  private static String mathFulfillmentBody(String submittedAnswer) {
    return """
        {
          "_class": "MATH_GAME",
          "submittedAnswer": "%s"
        }
        """
        .formatted(submittedAnswer);
  }

  private static String registerBody(String email) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "Math",
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

  private static String mathCreateBody(Instant notifyTime, Instant deadline) {
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

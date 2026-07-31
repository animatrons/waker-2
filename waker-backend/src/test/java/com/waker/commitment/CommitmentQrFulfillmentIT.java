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
import java.util.UUID;
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
class CommitmentQrFulfillmentIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";
  private static final String CODE_PAYLOAD = "kitchen-fridge-2026";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetRateLimits() {
    authRateLimiter.reset();
  }

  @Test
  void matchingQrPayloadFulfillsCommitment() throws Exception {
    String token = registerAndLogin("fulfill.happy@example.com");
    String commitmentId = createQrCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FULFILLED"))
        .andExpect(jsonPath("$.id").value(commitmentId));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
  }

  @Test
  void wrongPayloadReturns400AndStaysPending() throws Exception {
    String token = registerAndLogin("fulfill.wrong@example.com");
    String commitmentId = createQrCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody("wrong-payload")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void missedCommitmentReturns409() throws Exception {
    String token = registerAndLogin("fulfill.missed@example.com");
    String commitmentId = createQrCommitment(token);

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'MISSED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

    assertThat(commitmentStatus(commitmentId)).isEqualTo("MISSED");
  }

  @Test
  void fulfilledCommitmentReturns409() throws Exception {
    String token = registerAndLogin("fulfill.already@example.com");
    String commitmentId = createQrCommitment(token);

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'FULFILLED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isConflict());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("FULFILLED");
  }

  @Test
  void cancelledCommitmentReturns409() throws Exception {
    String token = registerAndLogin("fulfill.cancelled@example.com");
    String commitmentId = createQrCommitment(token);

    jdbcTemplate.update(
        "UPDATE commitments SET status = 'CANCELLED' WHERE id = ?::uuid", commitmentId);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isConflict());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("CANCELLED");
  }

  @Test
  void wrongOwnerReturns404() throws Exception {
    String tokenA = registerAndLogin("fulfill.ownera@example.com");
    String tokenB = registerAndLogin("fulfill.ownerb@example.com");
    String commitmentId = createQrCommitment(tokenB);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isNotFound());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  @Test
  void unauthenticatedReturns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/commitments/" + UUID.randomUUID() + "/fulfillment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(qrFulfillmentBody(CODE_PAYLOAD)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void blankScannedPayloadReturns400() throws Exception {
    String token = registerAndLogin("fulfill.blank@example.com");
    String commitmentId = createQrCommitment(token);

    mockMvc
        .perform(
            post("/api/v1/commitments/" + commitmentId + "/fulfillment")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "_class": "QR_CODE",
                      "scannedPayload": "   "
                    }
                    """))
        .andExpect(status().isBadRequest());

    assertThat(commitmentStatus(commitmentId)).isEqualTo("PENDING");
  }

  private String createQrCommitment(String token) throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        validCreateBody(
                            now.plus(1, ChronoUnit.HOURS),
                            now.plus(2, ChronoUnit.HOURS),
                            CODE_PAYLOAD)))
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

  private static String qrFulfillmentBody(String scannedPayload) {
    return """
        {
          "_class": "QR_CODE",
          "scannedPayload": "%s"
        }
        """
        .formatted(scannedPayload);
  }

  private static String registerBody(String email) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "Fulfill",
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

  private static String validCreateBody(Instant notifyTime, Instant deadline, String codePayload) {
    return """
        {
          "name": "Morning workout",
          "description": "Optional note",
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
            notifyTime.atOffset(ZoneOffset.UTC), deadline.atOffset(ZoneOffset.UTC), codePayload);
  }
}

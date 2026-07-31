package com.waker.commitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CommitmentQueryIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;

  @BeforeEach
  void resetRateLimits() {
    authRateLimiter.reset();
  }

  @Test
  void getOwnCommitmentByIdReturnsFullResponse() throws Exception {
    String token = registerAndLogin("query.own@example.com");
    String commitmentId = createCommitment(token);

    mockMvc
        .perform(
            get("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(commitmentId))
        .andExpect(jsonPath("$.name").value("Morning workout"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.missionConfig._class").value("QR_CODE"))
        .andExpect(jsonPath("$.penaltyConfig._class").value("EMAIL_TO_CONTACT"));
  }

  @Test
  void getOtherUsersCommitmentReturns404() throws Exception {
    String tokenA = registerAndLogin("query.usera@example.com");
    String tokenB = registerAndLogin("query.userb@example.com");
    String commitmentId = createCommitment(tokenB);

    mockMvc
        .perform(
            get("/api/v1/commitments/" + commitmentId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void getUnknownCommitmentReturns404() throws Exception {
    String token = registerAndLogin("query.unknown@example.com");
    UUID unknownId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/v1/commitments/" + unknownId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void listReturnsOnlyCallersCommitments() throws Exception {
    String tokenA = registerAndLogin("query.lista@example.com");
    String tokenB = registerAndLogin("query.listb@example.com");

    createCommitment(tokenA);
    createCommitment(tokenA);
    createCommitment(tokenB);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/commitments").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andReturn();

    JsonNode content =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    assertThat(content).hasSize(2);
    for (JsonNode item : content) {
      assertThat(item.get("name").asText()).isEqualTo("Morning workout");
    }
  }

  @Test
  void listFiltersByPendingStatus() throws Exception {
    String token = registerAndLogin("query.filter@example.com");
    String pendingId = createCommitment(token);

    mockMvc
        .perform(
            delete("/api/v1/commitments/" + pendingId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isNoContent());

    createCommitment(token);

    mockMvc
        .perform(
            get("/api/v1/commitments")
                .param("status", "PENDING")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("PENDING"));

    mockMvc
        .perform(
            get("/api/v1/commitments")
                .param("status", "CANCELLED")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
  }

  @Test
  void listPaginationWithSizeOneReturnsMultiplePages() throws Exception {
    String token = registerAndLogin("query.page@example.com");
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    createCommitment(token, now.plus(60, ChronoUnit.MINUTES), now.plus(90, ChronoUnit.MINUTES));
    createCommitment(token, now.plus(120, ChronoUnit.MINUTES), now.plus(150, ChronoUnit.MINUTES));

    mockMvc
        .perform(
            get("/api/v1/commitments")
                .param("size", "1")
                .param("page", "0")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.content.length()").value(1));

    mockMvc
        .perform(
            get("/api/v1/commitments")
                .param("size", "1")
                .param("page", "1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.content.length()").value(1));
  }

  @Test
  void listRejectsOversizePageRequest() throws Exception {
    String token = registerAndLogin("query.oversize@example.com");

    mockMvc
        .perform(
            get("/api/v1/commitments")
                .param("size", "101")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void unauthenticatedGetByIdReturns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/commitments/" + UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unauthenticatedListReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/commitments")).andExpect(status().isUnauthorized());
  }

  private String createCommitment(String token) throws Exception {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    return createCommitment(token, now.plus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
  }

  private String createCommitment(String token, Instant notifyTime, Instant deadline)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/commitments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validCreateBody(notifyTime, deadline)))
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
          "firstName": "Query",
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
}

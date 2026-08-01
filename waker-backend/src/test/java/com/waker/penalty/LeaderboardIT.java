package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import com.waker.common.AuthRateLimiter;
import java.time.Instant;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class LeaderboardIT extends AbstractIntegrationTest {

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
  void authenticatedListReturnsEntriesNewestFirst() throws Exception {
    jdbcTemplate.update("DELETE FROM leaderboard_entries");
    String token = registerAndLogin("lb.list@example.com");

    UUID olderCommitment = UUID.randomUUID();
    UUID newerCommitment = UUID.randomUUID();
    Instant olderMissed = Instant.parse("2026-07-30T06:00:00Z");
    Instant newerMissed = Instant.parse("2026-07-31T06:15:00Z");

    insertEntry(olderCommitment, "Older User", "Older commitment", olderMissed);
    insertEntry(newerCommitment, "Amin Example", "Morning workout", newerMissed);

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/leaderboard")
                    .param("page", "0")
                    .param("size", "20")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andReturn();

    JsonNode content =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
    assertThat(content.get(0).get("commitmentId").asText()).isEqualTo(newerCommitment.toString());
    assertThat(content.get(0).get("userDisplayName").asText()).isEqualTo("Amin Example");
    assertThat(content.get(0).get("commitmentName").asText()).isEqualTo("Morning workout");
    assertThat(content.get(0).get("missedAt").asText()).isEqualTo("2026-07-31T06:15:00Z");
    assertThat(content.get(1).get("commitmentId").asText()).isEqualTo(olderCommitment.toString());
  }

  @Test
  void unauthenticatedListReturns401() throws Exception {
    mockMvc.perform(get("/api/v1/leaderboard")).andExpect(status().isUnauthorized());
  }

  @Test
  void listRejectsOversizePageRequest() throws Exception {
    String token = registerAndLogin("lb.oversize@example.com");

    mockMvc
        .perform(
            get("/api/v1/leaderboard")
                .param("size", "101")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  private void insertEntry(
      UUID commitmentId, String displayName, String commitmentName, Instant missedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO leaderboard_entries (
            id, commitment_id, user_display_name, commitment_name, missed_at)
        VALUES (?::uuid, ?::uuid, ?, ?, ?::timestamptz)
        """,
        UUID.randomUUID().toString(),
        commitmentId.toString(),
        displayName,
        commitmentName,
        missedAt.toString());
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
          "firstName": "Board",
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
}

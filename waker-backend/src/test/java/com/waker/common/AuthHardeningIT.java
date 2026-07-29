package com.waker.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
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

/**
 * Epic 1 auth hardening gate (Story 1.6): register → login → JWT, anti-enumeration, 401 surface,
 * rate-limit 429, and CORS never {@code *}.
 *
 * <p>Shared suite capacity is high in {@link AbstractIntegrationTest}. The 429 path exhausts the
 * email bucket programmatically (capacity tokens) so the suite stays fast and non-flaky without a
 * separate ApplicationContext for low limits.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthHardeningIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthRateLimiter authRateLimiter;
  @Autowired private CorsProperties corsProperties;
  @Autowired private RateLimitProperties rateLimitProperties;

  @BeforeEach
  void resetRateLimits() {
    authRateLimiter.reset();
  }

  @Test
  void registerLoginThenBearerAuthenticatedRequestIsNot401() throws Exception {
    String email = "harden.flow@example.com";
    register(email, "Hard", "Flow");

    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

    String token =
        objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

    MvcResult protectedResult =
        mockMvc
            .perform(
                get("/api/v1/any-protected-path")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andReturn();

    assertThat(protectedResult.getResponse().getStatus()).isNotEqualTo(401);
  }

  @Test
  void duplicateEmailReturns409ProblemDetail() throws Exception {
    String email = "harden.dup@example.com";
    String body = registerBody(email, "Dup", "User");

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Email already registered"));
  }

  @Test
  void badEmailAndBadPasswordReturnIdentical401ProblemDetail() throws Exception {
    String email = "harden.wrong@example.com";
    register(email, "Wrong", "Pw");

    MvcResult wrongPassword =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, "wrong-password-xx")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Invalid email or password"))
            .andReturn();

    MvcResult unknownEmail =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("harden.nobody@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Invalid email or password"))
            .andReturn();

    JsonNode a = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
    JsonNode b = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
    assertThat(a.get("status")).isEqualTo(b.get("status"));
    assertThat(a.get("title").asText()).isEqualTo(b.get("title").asText());
    assertThat(a.get("detail").asText()).isEqualTo(b.get("detail").asText());
  }

  @Test
  void unauthenticatedProtectedPathReturns401ProblemDetail() throws Exception {
    mockMvc
        .perform(get("/api/v1/any-protected-path"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Authentication required"));
  }

  @Test
  void exceedingRateLimitReturns429AndSingleAttemptWithinLimitSucceeds() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("harden.ok@example.com", "Ok", "User")))
        .andExpect(status().isCreated());

    authRateLimiter.reset();

    String email = "harden.ratelimit@example.com";
    String emailKey = "email:" + email;
    for (long i = 0; i < rateLimitProperties.capacity(); i++) {
      authRateLimiter.checkOrThrow(emailKey);
    }

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginBody(email, PASSWORD)))
        .andExpect(status().isTooManyRequests())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too Many Requests"))
        .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Try again later."));
  }

  @Test
  void corsOriginsNeverContainWildcard() {
    assertThat(corsProperties.allowedOrigins()).isNotEmpty().doesNotContain("*");
  }

  private void register(String email, String firstName, String lastName) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(email, firstName, lastName)))
        .andExpect(status().isCreated());
  }

  private static String registerBody(String email, String firstName, String lastName) {
    return """
        {
          "email": "%s",
          "password": "%s",
          "firstName": "%s",
          "lastName": "%s"
        }
        """
        .formatted(email, PASSWORD, firstName, lastName);
  }

  private static String loginBody(String email, String password) {
    return """
        {
          "email": "%s",
          "password": "%s"
        }
        """
        .formatted(email, password);
  }
}

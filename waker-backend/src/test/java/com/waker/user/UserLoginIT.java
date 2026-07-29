package com.waker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class UserLoginIT extends AbstractIntegrationTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JwtDecoder jwtDecoder;
  @Autowired private JwtEncoder jwtEncoder;

  @Test
  void loginReturns200WithSignedJwt() throws Exception {
    String email = "login.happy@example.com";
    String userId = registerUser(email, "Happy", "Login");

    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(3600))
            .andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    String accessToken = body.get("accessToken").asText();
    var jwt = jwtDecoder.decode(accessToken);

    assertThat(jwt.getSubject()).isEqualTo(userId);
    assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
    assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
  }

  @Test
  void wrongPasswordAndUnknownEmailReturnIdenticalProblemDetail() throws Exception {
    String email = "login.wrongpw@example.com";
    registerUser(email, "Wrong", "Pw");

    MvcResult wrongPassword =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, "wrong-password-xx")))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Invalid email or password"))
            .andReturn();

    MvcResult unknownEmail =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody("nobody@example.com", PASSWORD)))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.detail").value("Invalid email or password"))
            .andReturn();

    JsonNode wrongBody = objectMapper.readTree(wrongPassword.getResponse().getContentAsString());
    JsonNode unknownBody = objectMapper.readTree(unknownEmail.getResponse().getContentAsString());
    assertThat(wrongBody.get("status")).isEqualTo(unknownBody.get("status"));
    assertThat(wrongBody.get("title").asText()).isEqualTo(unknownBody.get("title").asText());
    assertThat(wrongBody.get("detail").asText()).isEqualTo(unknownBody.get("detail").asText());
  }

  @Test
  void invalidLoginBodyReturns400ProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "not-an-email",
                      "password": "short"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Validation Failed"));
  }

  @Test
  void malformedBearerTokenOnProtectedPathReturns401ProblemDetail() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/any-protected-path").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Authentication required"));
  }

  @Test
  void expiredJwtOnProtectedPathReturns401ProblemDetail() throws Exception {
    Instant past = Instant.parse("2020-01-01T00:00:00Z");
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(UUID.randomUUID().toString())
            .claim("email", "expired@example.com")
            .issuedAt(past.minusSeconds(3600))
            .expiresAt(past)
            .build();
    String expiredToken =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();

    mockMvc
        .perform(
            get("/api/v1/any-protected-path")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Authentication required"));
  }

  @Test
  void validJwtOnUnknownProtectedPathIsNotUnauthenticated() throws Exception {
    String email = "login.bearer@example.com";
    registerUser(email, "Bearer", "User");

    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody(email, PASSWORD)))
            .andExpect(status().isOk())
            .andReturn();

    String accessToken =
        objectMapper.readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

    MvcResult protectedResult =
        mockMvc
            .perform(
                get("/api/v1/any-protected-path")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andReturn();

    assertThat(protectedResult.getResponse().getStatus()).isNotEqualTo(401);
  }

  private String registerUser(String email, String firstName, String lastName) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "email": "%s",
                          "password": "%s",
                          "firstName": "%s",
                          "lastName": "%s"
                        }
                        """
                            .formatted(email, PASSWORD, firstName, lastName)))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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

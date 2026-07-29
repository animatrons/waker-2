package com.waker.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.waker.AbstractIntegrationTest;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityContractIT extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private Clock clock;
  @Autowired private CorsProperties corsProperties;

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
  void actuatorHealthIsPermitAll() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void registerIsPermitAllForUnauthenticatedClients() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "security.permit@example.com",
                      "password": "correct-horse-battery",
                      "firstName": "Sec",
                      "lastName": "Test"
                    }
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  void allowedOriginReceivesCorsHeader() throws Exception {
    String allowed = corsProperties.allowedOrigins().getFirst();

    mockMvc
        .perform(
            options("/api/v1/any-protected-path")
                .header(ORIGIN, allowed)
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, allowed));
  }

  @Test
  void forbiddenOriginDoesNotReceiveWildcardCors() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/any-protected-path")
                .header(ORIGIN, "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void corsPropertiesNeverContainWildcard() {
    assertThat(corsProperties.allowedOrigins()).doesNotContain("*");
  }

  @Test
  void clockBeanIsPresentAndUtc() {
    assertThat(clock).isNotNull();
    assertThat(clock.getZone()).isEqualTo(java.time.ZoneOffset.UTC);
  }

  @Test
  void wildcardCorsPropertiesAreRejected() {
    assertThatThrownBy(() -> new CorsProperties(java.util.List.of("*")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("never contain '*'");
  }
}

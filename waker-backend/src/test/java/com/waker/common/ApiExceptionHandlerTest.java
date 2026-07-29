package com.waker.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();
    mockMvc =
        MockMvcBuilders.standaloneSetup(new StubValidationController())
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .build();
  }

  @Test
  void validationFailureReturnsProblemDetail() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.title").value("Validation Failed"))
        .andExpect(jsonPath("$.detail").isNotEmpty());
  }

  @Test
  void emailAlreadyRegisteredReturns409ProblemDetail() throws Exception {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new StubConflictController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(post("/api/v1/test/conflict"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.title").value("Conflict"))
        .andExpect(jsonPath("$.detail").value("Email already registered"));
  }

  @Test
  void invalidCredentialsReturns401ProblemDetail() throws Exception {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new StubUnauthorizedController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(post("/api/v1/test/unauthorized"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.title").value("Unauthorized"))
        .andExpect(jsonPath("$.detail").value("Invalid email or password"));
  }

  @Test
  void rateLimitExceededReturns429ProblemDetail() throws Exception {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new StubRateLimitController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    mockMvc
        .perform(post("/api/v1/test/rate-limit"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.status").value(429))
        .andExpect(jsonPath("$.title").value("Too Many Requests"))
        .andExpect(jsonPath("$.detail").value("Rate limit exceeded. Try again later."));
  }

  @RestController
  @RequestMapping("/api/v1/test")
  static class StubValidationController {

    @PostMapping("/validate")
    ResponseEntity<String> validate(@RequestBody @jakarta.validation.Valid StubRequest request) {
      return ResponseEntity.ok(request.name());
    }
  }

  record StubRequest(@NotBlank String name) {}

  @RestController
  @RequestMapping("/api/v1/test")
  static class StubConflictController {

    @PostMapping("/conflict")
    ResponseEntity<Void> conflict() {
      throw new EmailAlreadyRegisteredException();
    }
  }

  @RestController
  @RequestMapping("/api/v1/test")
  static class StubUnauthorizedController {

    @PostMapping("/unauthorized")
    ResponseEntity<Void> unauthorized() {
      throw new InvalidCredentialsException();
    }
  }

  @RestController
  @RequestMapping("/api/v1/test")
  static class StubRateLimitController {

    @PostMapping("/rate-limit")
    ResponseEntity<Void> rateLimit() {
      throw new RateLimitExceededException(30L);
    }
  }
}

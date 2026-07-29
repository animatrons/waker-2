package com.waker;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Postgres 17 container for the integration-test suite.
 *
 * <p>Starts once in a static initializer and is reused across subclasses. Do not annotate with
 * {@code @Testcontainers}/{@code @Container} — that lifecycle stops the container per test class
 * and defeats the singleton goal (Architecture testing convention).
 */
public abstract class AbstractIntegrationTest {

  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void registerDatasource(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("waker.jwt.secret", () -> "test-jwt-secret-at-least-32-chars-long!!");
    registry.add("waker.jwt.expiration", () -> "1h");
    // High enough that shared MockMvc remote IP does not exhaust across the suite.
    // AuthHardeningIT overrides to 3 via its own @DynamicPropertySource (subclass methods
    // register first; we re-register here last only for non-hardening ITs — hardening
    // uses DirtiesContext + its own property source set).
    registry.add("waker.rate-limit.capacity", () -> "1000");
    registry.add("waker.rate-limit.window", () -> "1m");
  }
}

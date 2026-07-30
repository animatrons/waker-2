package com.waker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ApplicationContextIT extends AbstractIntegrationTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoadsAgainstSharedPostgresWithFlywayApplied() {
    Integer applied =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);

    assertThat(applied).as("Flyway should have applied V1–V3 migrations").isGreaterThanOrEqualTo(3);
    assertThat(POSTGRES.isRunning()).as("singleton Postgres container must stay up").isTrue();
  }
}

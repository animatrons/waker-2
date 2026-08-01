package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class LeaderboardPenaltyDeliveryIT extends AbstractIntegrationTest {

  @Autowired private PenaltyEventLedger penaltyEventLedger;
  @Autowired private PenaltyDispatch penaltyDispatch;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void successfulDispatchRecordsLeaderboardEntryAndMarksLedgerDispatched() {
    UUID commitmentId = insertFixtureCommitment();
    LeaderboardPenaltyConfig config = new LeaderboardPenaltyConfig(true);
    PenaltyDispatchContext context = new PenaltyDispatchContext("Morning workout", "Amin Example");

    UUID eventId = penaltyEventLedger.insertPending(commitmentId, PenaltyType.LEADERBOARD);
    assertThat(penaltyEventLedger.claim(eventId)).isTrue();

    PenaltyDispatchResult result =
        penaltyDispatch.handlerFor(PenaltyType.LEADERBOARD).dispatch(commitmentId, config, context);

    assertThat(result.success()).isTrue();
    assertThat(penaltyEventLedger.markDispatched(eventId)).isTrue();
    assertThat(outcome(eventId)).isEqualTo("DISPATCHED");

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT commitment_id::text AS commitment_id,
                   user_display_name,
                   commitment_name,
                   missed_at
            FROM leaderboard_entries
            WHERE commitment_id = ?::uuid
            """,
            commitmentId.toString());
    assertThat(row.get("commitment_id")).isEqualTo(commitmentId.toString());
    assertThat(row.get("user_display_name")).isEqualTo("Amin Example");
    assertThat(row.get("commitment_name")).isEqualTo("Morning workout");
    assertThat(row.get("missed_at")).isNotNull();
  }

  @Test
  void consentFalseAtDispatchDoesNotInsertLeaderboardRow() {
    UUID commitmentId = insertFixtureCommitment();
    LeaderboardPenaltyConfig config = new LeaderboardPenaltyConfig(false);
    PenaltyDispatchContext context = new PenaltyDispatchContext("Morning workout", "Amin Example");

    PenaltyDispatchResult result =
        penaltyDispatch.handlerFor(PenaltyType.LEADERBOARD).dispatch(commitmentId, config, context);

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).containsIgnoringCase("consent");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaderboard_entries WHERE commitment_id = ?::uuid",
                Integer.class,
                commitmentId.toString()))
        .isZero();
  }

  private String outcome(UUID eventId) {
    return jdbcTemplate.queryForObject(
        "SELECT outcome FROM penalty_events WHERE id = ?::uuid", String.class, eventId.toString());
  }

  private UUID insertFixtureCommitment() {
    UUID userId = UUID.randomUUID();
    UUID commitmentId = UUID.randomUUID();
    Instant now = Instant.now();

    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at)
        VALUES (?::uuid, ?, 'hash', 'Board', 'Test', ?::timestamptz, ?::timestamptz)
        """,
        userId.toString(),
        "lb-" + userId + "@example.com",
        now.toString(),
        now.toString());

    jdbcTemplate.update(
        """
        INSERT INTO commitments (
            id, user_id, name, description, status, notify_time, deadline,
            mission_config, penalty_config, created_at, updated_at)
        VALUES (
            ?::uuid, ?::uuid, 'Leaderboard fixture', NULL, 'PENDING',
            ?::timestamptz, ?::timestamptz,
            '{"_class":"QR_CODE","codePayload":"fixture"}'::jsonb,
            '{"_class":"LEADERBOARD","consent":true}'::jsonb,
            ?::timestamptz, ?::timestamptz)
        """,
        commitmentId.toString(),
        userId.toString(),
        now.toString(),
        now.plusSeconds(3600).toString(),
        now.toString(),
        now.toString());

    return commitmentId;
  }
}

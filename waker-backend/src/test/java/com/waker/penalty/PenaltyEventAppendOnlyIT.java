package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.AbstractIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PenaltyEventAppendOnlyIT extends AbstractIntegrationTest {

  @Autowired private PenaltyEventLedger penaltyEventLedger;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void deleteIsForbiddenByTrigger() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "DELETE FROM penalty_events WHERE id = ?::uuid", eventId.toString()))
        .hasMessageContaining("append-only: DELETE is forbidden");
  }

  @Test
  void updatingCommitmentIdIsForbidden() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);
    UUID otherCommitment = insertFixtureCommitment();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE penalty_events SET commitment_id = ?::uuid WHERE id = ?::uuid",
                    otherCommitment.toString(),
                    eventId.toString()))
        .hasMessageContaining("only outcome may change");
  }

  @Test
  void updatingPenaltyTypeIsForbidden() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE penalty_events SET penalty_type = 'LEADERBOARD' WHERE id = ?::uuid",
                    eventId.toString()))
        .hasMessageContaining("only outcome may change");
  }

  @Test
  void skipClaimPendingToDispatchedIsForbidden() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.LEADERBOARD);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE penalty_events SET outcome = 'DISPATCHED' WHERE id = ?::uuid",
                    eventId.toString()))
        .hasMessageContaining("illegal outcome transition");
  }

  private UUID insertFixtureCommitment() {
    UUID userId = UUID.randomUUID();
    UUID commitmentId = UUID.randomUUID();
    Instant now = Instant.now();

    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at)
        VALUES (?::uuid, ?, 'hash', 'Append', 'Only', ?::timestamptz, ?::timestamptz)
        """,
        userId.toString(),
        "append-" + userId + "@example.com",
        now.toString(),
        now.toString());

    jdbcTemplate.update(
        """
        INSERT INTO commitments (
            id, user_id, name, description, status, notify_time, deadline,
            mission_config, penalty_config, created_at, updated_at)
        VALUES (
            ?::uuid, ?::uuid, 'Append fixture', NULL, 'PENDING',
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

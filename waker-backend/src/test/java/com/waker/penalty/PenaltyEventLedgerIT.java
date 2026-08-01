package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.AbstractIntegrationTest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class PenaltyEventLedgerIT extends AbstractIntegrationTest {

  @Autowired private PenaltyEventLedger penaltyEventLedger;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void insertPendingCreatesRowWithCorrectFields() {
    UUID commitmentId = insertFixtureCommitment();
    Instant before = Instant.now().minusSeconds(2);

    UUID eventId = penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);

    Instant after = Instant.now().plusSeconds(2);
    var row =
        jdbcTemplate.queryForMap(
            """
            SELECT commitment_id::text AS commitment_id,
                   penalty_type,
                   outcome,
                   created_at
            FROM penalty_events
            WHERE id = ?::uuid
            """,
            eventId.toString());

    assertThat(row.get("commitment_id")).isEqualTo(commitmentId.toString());
    assertThat(row.get("penalty_type")).isEqualTo("EMAIL_TO_CONTACT");
    assertThat(row.get("outcome")).isEqualTo("PENDING");
    Instant createdAt = ((java.sql.Timestamp) row.get("created_at")).toInstant();
    assertThat(createdAt).isBetween(before, after);
  }

  @Test
  void claimThenMarkDispatchedAndFailed() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.LEADERBOARD);

    assertThat(penaltyEventLedger.claim(eventId)).isTrue();
    assertThat(outcome(eventId)).isEqualTo("IN_PROGRESS");

    assertThat(penaltyEventLedger.claim(eventId)).isFalse();

    assertThat(penaltyEventLedger.markDispatched(eventId)).isTrue();
    assertThat(outcome(eventId)).isEqualTo("DISPATCHED");
    assertThat(penaltyEventLedger.markDispatched(eventId)).isFalse();

    UUID failedEventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);
    assertThat(penaltyEventLedger.claim(failedEventId)).isTrue();
    assertThat(penaltyEventLedger.markFailed(failedEventId)).isTrue();
    assertThat(outcome(failedEventId)).isEqualTo("FAILED");
    assertThat(penaltyEventLedger.markFailed(failedEventId)).isFalse();
  }

  @Test
  void illegalSkipClaimTransitionFailsAtDatabase() {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE penalty_events SET outcome = 'DISPATCHED' WHERE id = ?::uuid",
                    eventId.toString()))
        .hasMessageContaining("illegal outcome transition");
  }

  @Test
  void concurrentClaimersExactlyOneWins() throws Exception {
    UUID eventId =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.LEADERBOARD);

    int threads = 8;
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    CountDownLatch start = new CountDownLatch(1);
    AtomicInteger wins = new AtomicInteger();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    try {
      List<Future<?>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(
            pool.submit(
                () -> {
                  start.await();
                  Boolean won = tx.execute(status -> penaltyEventLedger.claim(eventId));
                  if (Boolean.TRUE.equals(won)) {
                    wins.incrementAndGet();
                  }
                  return null;
                }));
      }
      start.countDown();
      for (Future<?> future : futures) {
        future.get(10, TimeUnit.SECONDS);
      }
    } finally {
      pool.shutdownNow();
    }

    assertThat(wins.get()).isEqualTo(1);
    assertThat(outcome(eventId)).isEqualTo("IN_PROGRESS");
  }

  @Test
  void findPendingEventIdsReturnsOldestFirstCapped() {
    UUID older =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.EMAIL_TO_CONTACT);
    UUID newer =
        penaltyEventLedger.insertPending(insertFixtureCommitment(), PenaltyType.LEADERBOARD);
    penaltyEventLedger.claim(newer);

    List<UUID> pending = penaltyEventLedger.findPendingEventIds(10);

    assertThat(pending).contains(older).doesNotContain(newer);
    assertThat(penaltyEventLedger.findPendingEventIds(1)).hasSize(1);
    assertThat(penaltyEventLedger.findPendingEventIds(0)).isEmpty();
  }

  @Test
  void findPendingEventsReturnsOldestFirstWithTypeAndCommitment() {
    UUID olderCommitment = insertFixtureCommitment();
    UUID newerCommitment = insertFixtureCommitment();
    UUID older = penaltyEventLedger.insertPending(olderCommitment, PenaltyType.EMAIL_TO_CONTACT);
    UUID newer = penaltyEventLedger.insertPending(newerCommitment, PenaltyType.LEADERBOARD);
    penaltyEventLedger.claim(newer);

    List<PenaltyEventLedger.PendingPenaltyEvent> pending = penaltyEventLedger.findPendingEvents(50);

    assertThat(pending)
        .anySatisfy(
            event -> {
              assertThat(event.id()).isEqualTo(older);
              assertThat(event.commitmentId()).isEqualTo(olderCommitment);
              assertThat(event.penaltyType()).isEqualTo(PenaltyType.EMAIL_TO_CONTACT);
            });
    assertThat(pending).noneMatch(event -> event.id().equals(newer));
    assertThat(penaltyEventLedger.findPendingEvents(0)).isEmpty();
  }

  @Test
  void uniqueCommitmentIdRejectsSecondPendingRow() {
    UUID commitmentId = insertFixtureCommitment();
    penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);

    assertThatThrownBy(
            () -> penaltyEventLedger.insertPending(commitmentId, PenaltyType.LEADERBOARD))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void sameTransactionCommitPersistsMissedStatusAndPendingEvent() {
    UUID commitmentId = insertFixtureCommitment();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    UUID eventId =
        tx.execute(
            status -> {
              jdbcTemplate.update(
                  "UPDATE commitments SET status = 'MISSED' WHERE id = ?::uuid",
                  commitmentId.toString());
              return penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);
            });

    assertThat(eventId).isNotNull();
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM commitments WHERE id = ?::uuid",
                String.class,
                commitmentId.toString()))
        .isEqualTo("MISSED");
    assertThat(outcome(eventId)).isEqualTo("PENDING");
  }

  @Test
  void sameTransactionRollbackLeavesNeitherMissedNorEvent() {
    UUID commitmentId = insertFixtureCommitment();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);

    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    status -> {
                      jdbcTemplate.update(
                          "UPDATE commitments SET status = 'MISSED' WHERE id = ?::uuid",
                          commitmentId.toString());
                      penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);
                      throw new RuntimeException("force rollback");
                    }))
        .hasMessage("force rollback");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT status FROM commitments WHERE id = ?::uuid",
                String.class,
                commitmentId.toString()))
        .isEqualTo("PENDING");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM penalty_events WHERE commitment_id = ?::uuid",
                Integer.class,
                commitmentId.toString()))
        .isEqualTo(0);
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
        VALUES (?::uuid, ?, 'hash', 'Ledger', 'Test', ?::timestamptz, ?::timestamptz)
        """,
        userId.toString(),
        "ledger-" + userId + "@example.com",
        now.toString(),
        now.toString());

    jdbcTemplate.update(
        """
        INSERT INTO commitments (
            id, user_id, name, description, status, notify_time, deadline,
            mission_config, penalty_config, created_at, updated_at)
        VALUES (
            ?::uuid, ?::uuid, 'Ledger fixture', NULL, 'PENDING',
            ?::timestamptz, ?::timestamptz,
            '{"_class":"QR_CODE","codePayload":"fixture"}'::jsonb,
            '{"_class":"EMAIL_TO_CONTACT","contactEmail":"a@b.co","message":"hi"}'::jsonb,
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

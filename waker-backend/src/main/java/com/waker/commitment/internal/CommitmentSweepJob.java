package com.waker.commitment.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class CommitmentSweepJob {

  private static final Logger log = LoggerFactory.getLogger(CommitmentSweepJob.class);

  private final Clock clock;
  private final CommitmentRepository commitmentRepository;
  private final CommitmentMissEnforcer missEnforcer;
  private final PenaltyDispatcher penaltyDispatcher;

  CommitmentSweepJob(
      @Qualifier("commitmentClock") Clock clock,
      CommitmentRepository commitmentRepository,
      CommitmentMissEnforcer missEnforcer,
      PenaltyDispatcher penaltyDispatcher) {
    this.clock = clock;
    this.commitmentRepository = commitmentRepository;
    this.missEnforcer = missEnforcer;
    this.penaltyDispatcher = penaltyDispatcher;
  }

  /** Same cadence: miss-pass then dispatch-pass (AD-5 / Story 3.5). */
  @Scheduled(fixedDelayString = "${waker.sweep.interval}")
  void scheduledCycle() {
    runSweep();
    runDispatch();
  }

  /** Miss detection only — used by tests that assert PENDING outbox before dispatch. */
  void runSweep() {
    Instant now = Instant.now(clock);
    List<UUID> overdueIds = commitmentRepository.findOverduePendingIds(now);
    for (UUID commitmentId : overdueIds) {
      try {
        missEnforcer.markMissedAndEnqueue(commitmentId, now);
      } catch (RuntimeException ex) {
        log.error("Sweep failed for commitmentId={}", commitmentId, ex);
      }
    }
  }

  /** Outbox relay only — used by reliability ITs and after miss-pass in production. */
  void runDispatch() {
    penaltyDispatcher.dispatchPending();
  }
}

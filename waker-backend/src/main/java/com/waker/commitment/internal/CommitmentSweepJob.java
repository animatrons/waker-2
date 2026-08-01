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

  CommitmentSweepJob(
      @Qualifier("commitmentClock") Clock clock,
      CommitmentRepository commitmentRepository,
      CommitmentMissEnforcer missEnforcer) {
    this.clock = clock;
    this.commitmentRepository = commitmentRepository;
    this.missEnforcer = missEnforcer;
  }

  @Scheduled(fixedDelayString = "${waker.sweep.interval}")
  void sweep() {
    runSweep();
  }

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
}

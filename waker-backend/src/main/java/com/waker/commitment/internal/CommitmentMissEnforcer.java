package com.waker.commitment.internal;

import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.LeaderboardPenaltyConfig;
import com.waker.penalty.PenaltyConfig;
import com.waker.penalty.PenaltyEventLedger;
import com.waker.penalty.PenaltyType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommitmentMissEnforcer {

  private final CommitmentRepository commitmentRepository;
  private final PenaltyEventLedger penaltyEventLedger;

  CommitmentMissEnforcer(
      CommitmentRepository commitmentRepository, PenaltyEventLedger penaltyEventLedger) {
    this.commitmentRepository = commitmentRepository;
    this.penaltyEventLedger = penaltyEventLedger;
  }

  @Transactional
  void markMissedAndEnqueue(UUID commitmentId, Instant now) {
    Commitment commitment = commitmentRepository.findById(commitmentId).orElse(null);
    if (commitment == null) {
      return;
    }

    PenaltyType penaltyType = penaltyTypeOf(commitment.getPenaltyConfig());
    int updated = commitmentRepository.markMissedIfPending(commitmentId, now);
    if (updated == 1) {
      penaltyEventLedger.insertPending(commitmentId, penaltyType);
    }
  }

  private static PenaltyType penaltyTypeOf(PenaltyConfig config) {
    return switch (config) {
      case EmailToContactPenaltyConfig ignored -> PenaltyType.EMAIL_TO_CONTACT;
      case LeaderboardPenaltyConfig ignored -> PenaltyType.LEADERBOARD;
    };
  }
}

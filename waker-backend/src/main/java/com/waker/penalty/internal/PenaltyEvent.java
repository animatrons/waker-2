package com.waker.penalty.internal;

import com.waker.penalty.PenaltyEventOutcome;
import com.waker.penalty.PenaltyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "penalty_events")
class PenaltyEvent {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  /** Bare UUID scalar — no JPA relation to Commitment (AD-2). */
  @Column(name = "commitment_id", nullable = false, updatable = false)
  private UUID commitmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "penalty_type", nullable = false, updatable = false, length = 40)
  private PenaltyType penaltyType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PenaltyEventOutcome outcome;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  protected PenaltyEvent() {}

  PenaltyEvent(
      UUID id,
      UUID commitmentId,
      PenaltyType penaltyType,
      PenaltyEventOutcome outcome,
      Instant createdAt) {
    this.id = id;
    this.commitmentId = commitmentId;
    this.penaltyType = penaltyType;
    this.outcome = outcome;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCommitmentId() {
    return commitmentId;
  }

  public PenaltyType getPenaltyType() {
    return penaltyType;
  }

  public PenaltyEventOutcome getOutcome() {
    return outcome;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

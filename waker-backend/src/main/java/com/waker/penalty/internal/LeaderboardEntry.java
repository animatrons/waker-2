package com.waker.penalty.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "leaderboard_entries")
class LeaderboardEntry {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  /** Bare UUID scalar — no JPA relation to Commitment (AD-2). */
  @Column(name = "commitment_id", nullable = false, updatable = false)
  private UUID commitmentId;

  @Column(name = "user_display_name", nullable = false, updatable = false, length = 201)
  private String userDisplayName;

  @Column(name = "commitment_name", nullable = false, updatable = false, length = 200)
  private String commitmentName;

  @Column(name = "missed_at", nullable = false, updatable = false)
  private Instant missedAt;

  protected LeaderboardEntry() {}

  LeaderboardEntry(
      UUID id, UUID commitmentId, String userDisplayName, String commitmentName, Instant missedAt) {
    this.id = id;
    this.commitmentId = commitmentId;
    this.userDisplayName = userDisplayName;
    this.commitmentName = commitmentName;
    this.missedAt = missedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCommitmentId() {
    return commitmentId;
  }

  public String getUserDisplayName() {
    return userDisplayName;
  }

  public String getCommitmentName() {
    return commitmentName;
  }

  public Instant getMissedAt() {
    return missedAt;
  }
}

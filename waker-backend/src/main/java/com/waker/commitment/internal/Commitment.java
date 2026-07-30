package com.waker.commitment.internal;

import com.waker.commitment.CommitmentStatus;
import com.waker.mission.MissionConfig;
import com.waker.penalty.PenaltyConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "commitments")
class Commitment {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 1000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CommitmentStatus status;

  @Column(name = "notify_time", nullable = false)
  private Instant notifyTime;

  @Column(nullable = false)
  private Instant deadline;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "mission_config", columnDefinition = "jsonb", nullable = false)
  private MissionConfig missionConfig;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "penalty_config", columnDefinition = "jsonb", nullable = false)
  private PenaltyConfig penaltyConfig;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Commitment() {}

  Commitment(
      UUID id,
      UUID userId,
      String name,
      String description,
      CommitmentStatus status,
      Instant notifyTime,
      Instant deadline,
      MissionConfig missionConfig,
      PenaltyConfig penaltyConfig,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.userId = userId;
    this.name = name;
    this.description = description;
    this.status = status;
    this.notifyTime = notifyTime;
    this.deadline = deadline;
    this.missionConfig = missionConfig;
    this.penaltyConfig = penaltyConfig;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public CommitmentStatus getStatus() {
    return status;
  }

  public Instant getNotifyTime() {
    return notifyTime;
  }

  public Instant getDeadline() {
    return deadline;
  }

  public MissionConfig getMissionConfig() {
    return missionConfig;
  }

  public PenaltyConfig getPenaltyConfig() {
    return penaltyConfig;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

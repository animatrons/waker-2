package com.waker.commitment.internal;

import com.waker.commitment.CommitmentStatus;
import com.waker.mission.MissionConfig;
import com.waker.penalty.PenaltyConfig;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

  long countByUserIdAndStatus(UUID userId, CommitmentStatus status);

  Optional<Commitment> findByIdAndUserId(UUID id, UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE Commitment c
      SET c.status = com.waker.commitment.CommitmentStatus.CANCELLED,
          c.updatedAt = :now
      WHERE c.id = :id
        AND c.userId = :userId
        AND c.status = com.waker.commitment.CommitmentStatus.PENDING
      """)
  int cancelIfPending(
      @Param("id") UUID id, @Param("userId") UUID userId, @Param("now") Instant now);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE Commitment c
      SET c.name = :name,
          c.description = :description,
          c.notifyTime = :notifyTime,
          c.deadline = :deadline,
          c.missionConfig = :missionConfig,
          c.penaltyConfig = :penaltyConfig,
          c.updatedAt = :now
      WHERE c.id = :id
        AND c.userId = :userId
        AND c.status = com.waker.commitment.CommitmentStatus.PENDING
      """)
  int updateIfPending(
      @Param("id") UUID id,
      @Param("userId") UUID userId,
      @Param("name") String name,
      @Param("description") String description,
      @Param("notifyTime") Instant notifyTime,
      @Param("deadline") Instant deadline,
      @Param("missionConfig") MissionConfig missionConfig,
      @Param("penaltyConfig") PenaltyConfig penaltyConfig,
      @Param("now") Instant now);
}

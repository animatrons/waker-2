package com.waker.penalty.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PenaltyEventRepository extends JpaRepository<PenaltyEvent, UUID> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE PenaltyEvent e
      SET e.outcome = com.waker.penalty.PenaltyEventOutcome.IN_PROGRESS
      WHERE e.id = :id
        AND e.outcome = com.waker.penalty.PenaltyEventOutcome.PENDING
      """)
  int claimIfPending(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE PenaltyEvent e
      SET e.outcome = com.waker.penalty.PenaltyEventOutcome.DISPATCHED
      WHERE e.id = :id
        AND e.outcome = com.waker.penalty.PenaltyEventOutcome.IN_PROGRESS
      """)
  int markDispatchedIfInProgress(@Param("id") UUID id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      UPDATE PenaltyEvent e
      SET e.outcome = com.waker.penalty.PenaltyEventOutcome.FAILED
      WHERE e.id = :id
        AND e.outcome = com.waker.penalty.PenaltyEventOutcome.IN_PROGRESS
      """)
  int markFailedIfInProgress(@Param("id") UUID id);

  @Query(
      """
      SELECT e.id FROM PenaltyEvent e
      WHERE e.outcome = com.waker.penalty.PenaltyEventOutcome.PENDING
      ORDER BY e.createdAt ASC
      """)
  List<UUID> findPendingIds(Pageable pageable);
}

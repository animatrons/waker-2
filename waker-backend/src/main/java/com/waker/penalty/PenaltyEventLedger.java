package com.waker.penalty;

import java.util.List;
import java.util.UUID;

/**
 * Append-only penalty outbox ledger (AD-5 / FR-13 / FR-17).
 *
 * <p>Callers (Story 3.4/3.5) use only this public surface — never the repository.
 */
public interface PenaltyEventLedger {

  /** Insert outcome=PENDING. Joins caller's transaction. Returns new event id. */
  UUID insertPending(UUID commitmentId, PenaltyType penaltyType);

  /** Atomic PENDING → IN_PROGRESS. Returns true iff this caller won the claim. */
  boolean claim(UUID eventId);

  /** Atomic IN_PROGRESS → DISPATCHED. Returns true iff one row updated. */
  boolean markDispatched(UUID eventId);

  /** Atomic IN_PROGRESS → FAILED. Returns true iff one row updated. */
  boolean markFailed(UUID eventId);

  /** Oldest-first PENDING ids, capped — for dispatcher pass (Story 3.5). */
  List<UUID> findPendingEventIds(int limit);
}

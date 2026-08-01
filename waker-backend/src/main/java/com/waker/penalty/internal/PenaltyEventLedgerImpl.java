package com.waker.penalty.internal;

import com.waker.penalty.PenaltyEventLedger;
import com.waker.penalty.PenaltyEventOutcome;
import com.waker.penalty.PenaltyType;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PenaltyEventLedgerImpl implements PenaltyEventLedger {

  private final PenaltyEventRepository penaltyEventRepository;
  private final Clock clock;

  PenaltyEventLedgerImpl(PenaltyEventRepository penaltyEventRepository, Clock clock) {
    this.penaltyEventRepository = penaltyEventRepository;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UUID insertPending(UUID commitmentId, PenaltyType penaltyType) {
    UUID id = UUID.randomUUID();
    PenaltyEvent event =
        new PenaltyEvent(
            id, commitmentId, penaltyType, PenaltyEventOutcome.PENDING, Instant.now(clock));
    penaltyEventRepository.save(event);
    return id;
  }

  @Override
  @Transactional
  public boolean claim(UUID eventId) {
    return penaltyEventRepository.claimIfPending(eventId) == 1;
  }

  @Override
  @Transactional
  public boolean markDispatched(UUID eventId) {
    return penaltyEventRepository.markDispatchedIfInProgress(eventId) == 1;
  }

  @Override
  @Transactional
  public boolean markFailed(UUID eventId) {
    return penaltyEventRepository.markFailedIfInProgress(eventId) == 1;
  }

  @Override
  @Transactional(readOnly = true)
  public List<UUID> findPendingEventIds(int limit) {
    if (limit < 1) {
      return List.of();
    }
    return penaltyEventRepository.findPendingIds(PageRequest.of(0, limit));
  }

  @Override
  @Transactional(readOnly = true)
  public List<PendingPenaltyEvent> findPendingEvents(int limit) {
    if (limit < 1) {
      return List.of();
    }
    return penaltyEventRepository.findPending(PageRequest.of(0, limit)).stream()
        .map(
            event ->
                new PendingPenaltyEvent(
                    event.getId(), event.getCommitmentId(), event.getPenaltyType()))
        .toList();
  }
}

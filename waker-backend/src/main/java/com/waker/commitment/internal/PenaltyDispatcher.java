package com.waker.commitment.internal;

import com.waker.commitment.SweepProperties;
import com.waker.penalty.PenaltyDispatch;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import com.waker.penalty.PenaltyEventLedger;
import com.waker.penalty.PenaltyEventLedger.PendingPenaltyEvent;
import com.waker.user.UserResponse;
import com.waker.user.UserService;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Production outbox relay: claim → handler → terminal outcome (Story 3.5 / FR-17).
 *
 * <p>Side effects run outside the claim transaction so SMTP/leaderboard work cannot hold row locks
 * or resurrect PENDING by rolling back a successful send.
 */
@Service
class PenaltyDispatcher {

  private static final Logger log = LoggerFactory.getLogger(PenaltyDispatcher.class);

  private final PenaltyEventLedger penaltyEventLedger;
  private final PenaltyDispatch penaltyDispatch;
  private final CommitmentRepository commitmentRepository;
  private final UserService userService;
  private final SweepProperties sweepProperties;

  PenaltyDispatcher(
      PenaltyEventLedger penaltyEventLedger,
      PenaltyDispatch penaltyDispatch,
      CommitmentRepository commitmentRepository,
      UserService userService,
      SweepProperties sweepProperties) {
    this.penaltyEventLedger = penaltyEventLedger;
    this.penaltyDispatch = penaltyDispatch;
    this.commitmentRepository = commitmentRepository;
    this.userService = userService;
    this.sweepProperties = sweepProperties;
  }

  void dispatchPending() {
    List<PendingPenaltyEvent> pending =
        penaltyEventLedger.findPendingEvents(sweepProperties.dispatchBatchSize());
    for (PendingPenaltyEvent event : pending) {
      try {
        dispatchOne(event);
      } catch (RuntimeException ex) {
        log.error("Unexpected dispatcher failure for eventId={}", event.id(), ex);
      }
    }
  }

  private void dispatchOne(PendingPenaltyEvent event) {
    Commitment commitment = commitmentRepository.findById(event.commitmentId()).orElse(null);
    if (commitment == null) {
      log.error(
          "Missing commitment for pending penalty eventId={} commitmentId={}",
          event.id(),
          event.commitmentId());
      claimAndMarkFailed(event.id());
      return;
    }

    UserResponse user;
    try {
      user = userService.getById(commitment.getUserId());
    } catch (RuntimeException ex) {
      log.error(
          "Missing user for pending penalty eventId={} userId={}",
          event.id(),
          commitment.getUserId(),
          ex);
      claimAndMarkFailed(event.id());
      return;
    }

    PenaltyDispatchContext context =
        new PenaltyDispatchContext(commitment.getName(), displayName(user));

    if (!penaltyEventLedger.claim(event.id())) {
      return;
    }

    try {
      PenaltyDispatchResult result =
          penaltyDispatch
              .handlerFor(event.penaltyType())
              .dispatch(event.commitmentId(), commitment.getPenaltyConfig(), context);
      if (result.success()) {
        penaltyEventLedger.markDispatched(event.id());
      } else {
        log.error(
            "Penalty handler reported failure eventId={} commitmentId={} detail={}",
            event.id(),
            event.commitmentId(),
            result.detail());
        penaltyEventLedger.markFailed(event.id());
      }
    } catch (RuntimeException ex) {
      log.error(
          "Penalty handler threw for eventId={} commitmentId={}",
          event.id(),
          event.commitmentId(),
          ex);
      penaltyEventLedger.markFailed(event.id());
    }
  }

  private void claimAndMarkFailed(UUID eventId) {
    if (penaltyEventLedger.claim(eventId)) {
      penaltyEventLedger.markFailed(eventId);
    }
  }

  static String displayName(UserResponse user) {
    String first = user.firstName() == null ? "" : user.firstName().trim();
    String last = user.lastName() == null ? "" : user.lastName().trim();
    String name = (first + " " + last).trim();
    return name.isEmpty() ? user.email() : name;
  }
}

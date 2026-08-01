package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.waker.commitment.CommitmentStatus;
import com.waker.commitment.SweepProperties;
import com.waker.mission.QrCodeMissionConfig;
import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.PenaltyDispatch;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import com.waker.penalty.PenaltyEventLedger;
import com.waker.penalty.PenaltyEventLedger.PendingPenaltyEvent;
import com.waker.penalty.PenaltyHandler;
import com.waker.penalty.PenaltyType;
import com.waker.user.UserNotFoundException;
import com.waker.user.UserResponse;
import com.waker.user.UserService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PenaltyDispatcherTest {

  private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID COMMITMENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID USER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Mock private PenaltyEventLedger penaltyEventLedger;
  @Mock private PenaltyDispatch penaltyDispatch;
  @Mock private CommitmentRepository commitmentRepository;
  @Mock private UserService userService;
  @Mock private PenaltyHandler handler;

  private PenaltyDispatcher dispatcher;

  @BeforeEach
  void setUp() {
    dispatcher =
        new PenaltyDispatcher(
            penaltyEventLedger,
            penaltyDispatch,
            commitmentRepository,
            userService,
            new SweepProperties(Duration.ofSeconds(60), 50));
  }

  @Test
  void claimThenSuccessfulDispatchMarksDispatched() {
    PendingPenaltyEvent pending =
        new PendingPenaltyEvent(EVENT_ID, COMMITMENT_ID, PenaltyType.EMAIL_TO_CONTACT);
    Commitment commitment = sampleCommitment();
    EmailToContactPenaltyConfig config =
        (EmailToContactPenaltyConfig) commitment.getPenaltyConfig();

    when(penaltyEventLedger.findPendingEvents(50)).thenReturn(List.of(pending));
    when(commitmentRepository.findById(COMMITMENT_ID)).thenReturn(Optional.of(commitment));
    when(userService.getById(USER_ID))
        .thenReturn(new UserResponse(USER_ID, "a@example.com", "Amin", "Example", Instant.now()));
    when(penaltyEventLedger.claim(EVENT_ID)).thenReturn(true);
    when(penaltyDispatch.handlerFor(PenaltyType.EMAIL_TO_CONTACT)).thenReturn(handler);
    when(handler.dispatch(eq(COMMITMENT_ID), eq(config), any(PenaltyDispatchContext.class)))
        .thenReturn(PenaltyDispatchResult.success("ok"));

    dispatcher.dispatchPending();

    ArgumentCaptor<PenaltyDispatchContext> contextCaptor =
        ArgumentCaptor.forClass(PenaltyDispatchContext.class);
    verify(handler).dispatch(eq(COMMITMENT_ID), eq(config), contextCaptor.capture());
    assertThat(contextCaptor.getValue().commitmentName()).isEqualTo("Morning run");
    assertThat(contextCaptor.getValue().userDisplayName()).isEqualTo("Amin Example");
    verify(penaltyEventLedger).markDispatched(EVENT_ID);
    verify(penaltyEventLedger, never()).markFailed(EVENT_ID);
  }

  @Test
  void lostClaimDoesNotInvokeHandler() {
    PendingPenaltyEvent pending =
        new PendingPenaltyEvent(EVENT_ID, COMMITMENT_ID, PenaltyType.EMAIL_TO_CONTACT);
    when(penaltyEventLedger.findPendingEvents(50)).thenReturn(List.of(pending));
    when(commitmentRepository.findById(COMMITMENT_ID)).thenReturn(Optional.of(sampleCommitment()));
    when(userService.getById(USER_ID))
        .thenReturn(new UserResponse(USER_ID, "a@example.com", "Amin", "Example", Instant.now()));
    when(penaltyEventLedger.claim(EVENT_ID)).thenReturn(false);

    dispatcher.dispatchPending();

    verify(penaltyDispatch, never()).handlerFor(any());
    verify(penaltyEventLedger, never()).markDispatched(any());
    verify(penaltyEventLedger, never()).markFailed(any());
  }

  @Test
  void handlerFailureMarksFailed() {
    PendingPenaltyEvent pending =
        new PendingPenaltyEvent(EVENT_ID, COMMITMENT_ID, PenaltyType.EMAIL_TO_CONTACT);
    when(penaltyEventLedger.findPendingEvents(50)).thenReturn(List.of(pending));
    when(commitmentRepository.findById(COMMITMENT_ID)).thenReturn(Optional.of(sampleCommitment()));
    when(userService.getById(USER_ID))
        .thenReturn(new UserResponse(USER_ID, "a@example.com", "Amin", "Example", Instant.now()));
    when(penaltyEventLedger.claim(EVENT_ID)).thenReturn(true);
    when(penaltyDispatch.handlerFor(PenaltyType.EMAIL_TO_CONTACT)).thenReturn(handler);
    when(handler.dispatch(any(), any(), any()))
        .thenReturn(PenaltyDispatchResult.failure("smtp down"));

    dispatcher.dispatchPending();

    verify(penaltyEventLedger).markFailed(EVENT_ID);
    verify(penaltyEventLedger, never()).markDispatched(EVENT_ID);
  }

  @Test
  void handlerThrowMarksFailedWithoutAbortingBatch() {
    UUID secondEvent = UUID.fromString("44444444-4444-4444-4444-444444444444");
    UUID secondCommitment = UUID.fromString("55555555-5555-5555-5555-555555555555");
    PendingPenaltyEvent first =
        new PendingPenaltyEvent(EVENT_ID, COMMITMENT_ID, PenaltyType.EMAIL_TO_CONTACT);
    PendingPenaltyEvent second =
        new PendingPenaltyEvent(secondEvent, secondCommitment, PenaltyType.EMAIL_TO_CONTACT);
    Commitment secondCommitmentEntity =
        new Commitment(
            secondCommitment,
            USER_ID,
            "Second",
            null,
            CommitmentStatus.MISSED,
            Instant.parse("2026-08-01T12:15:00Z"),
            Instant.parse("2026-08-01T12:30:00Z"),
            new QrCodeMissionConfig("payload"),
            new EmailToContactPenaltyConfig("friend@example.com", "missed"),
            Instant.parse("2026-08-01T12:00:00Z"),
            Instant.parse("2026-08-01T12:30:00Z"));

    when(penaltyEventLedger.findPendingEvents(50)).thenReturn(List.of(first, second));
    when(commitmentRepository.findById(COMMITMENT_ID)).thenReturn(Optional.of(sampleCommitment()));
    when(commitmentRepository.findById(secondCommitment))
        .thenReturn(Optional.of(secondCommitmentEntity));
    when(userService.getById(USER_ID))
        .thenReturn(new UserResponse(USER_ID, "a@example.com", "Amin", "Example", Instant.now()));
    when(penaltyEventLedger.claim(EVENT_ID)).thenReturn(true);
    when(penaltyEventLedger.claim(secondEvent)).thenReturn(true);
    when(penaltyDispatch.handlerFor(PenaltyType.EMAIL_TO_CONTACT)).thenReturn(handler);
    when(handler.dispatch(eq(COMMITMENT_ID), any(), any())).thenThrow(new RuntimeException("boom"));
    when(handler.dispatch(eq(secondCommitment), any(), any()))
        .thenReturn(PenaltyDispatchResult.success("ok"));

    dispatcher.dispatchPending();

    verify(penaltyEventLedger).markFailed(EVENT_ID);
    verify(penaltyEventLedger).markDispatched(secondEvent);
  }

  @Test
  void missingUserClaimsAndMarksFailed() {
    PendingPenaltyEvent pending =
        new PendingPenaltyEvent(EVENT_ID, COMMITMENT_ID, PenaltyType.EMAIL_TO_CONTACT);
    when(penaltyEventLedger.findPendingEvents(50)).thenReturn(List.of(pending));
    when(commitmentRepository.findById(COMMITMENT_ID)).thenReturn(Optional.of(sampleCommitment()));
    when(userService.getById(USER_ID)).thenThrow(new UserNotFoundException());
    when(penaltyEventLedger.claim(EVENT_ID)).thenReturn(true);

    dispatcher.dispatchPending();

    verify(penaltyEventLedger).markFailed(EVENT_ID);
    verify(penaltyDispatch, never()).handlerFor(any());
  }

  @Test
  void displayNameFallsBackToEmailWhenNamesBlank() {
    UserResponse user = new UserResponse(USER_ID, "solo@example.com", "  ", "", Instant.now());
    assertThat(PenaltyDispatcher.displayName(user)).isEqualTo("solo@example.com");
  }

  private static Commitment sampleCommitment() {
    return new Commitment(
        COMMITMENT_ID,
        USER_ID,
        "Morning run",
        null,
        CommitmentStatus.MISSED,
        Instant.parse("2026-08-01T12:15:00Z"),
        Instant.parse("2026-08-01T12:30:00Z"),
        new QrCodeMissionConfig("payload"),
        new EmailToContactPenaltyConfig("friend@example.com", "I missed my commitment."),
        Instant.parse("2026-08-01T12:00:00Z"),
        Instant.parse("2026-08-01T12:30:00Z"));
  }
}

package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class CommitmentSweepJobTest {

  private static final Instant FIXED = Instant.parse("2026-08-01T12:00:00Z");

  @Mock private CommitmentRepository commitmentRepository;
  @Mock private CommitmentMissEnforcer missEnforcer;
  @Mock private PenaltyDispatcher penaltyDispatcher;
  @Mock private SweepHealthState sweepHealthState;

  private CommitmentSweepJob job;
  private ListAppender<ILoggingEvent> appender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    job =
        new CommitmentSweepJob(
            Clock.fixed(FIXED, ZoneOffset.UTC),
            commitmentRepository,
            missEnforcer,
            penaltyDispatcher,
            sweepHealthState);
    logger = (Logger) LoggerFactory.getLogger(CommitmentSweepJob.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  @Test
  void successfulCycleMarksHealthSuccess() {
    when(commitmentRepository.findOverduePendingIds(FIXED)).thenReturn(List.of());

    job.scheduledCycle();

    verify(penaltyDispatcher).dispatchPending();
    verify(sweepHealthState).markSuccess();
    verify(sweepHealthState, never()).markFailure(any());
  }

  @Test
  void topLevelFailureDoesNotMarkSuccessAndLogsError() {
    when(commitmentRepository.findOverduePendingIds(FIXED))
        .thenThrow(new RuntimeException("repository down"));

    job.scheduledCycle();

    verify(sweepHealthState, never()).markSuccess();
    verify(sweepHealthState).markFailure(any(RuntimeException.class));
    verify(penaltyDispatcher, never()).dispatchPending();

    assertThat(appender.list)
        .anyMatch(
            event ->
                event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().contains("Sweep cycle failed"));
  }

  @Test
  void perRowFailureStillMarksSuccess() {
    UUID commitmentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    when(commitmentRepository.findOverduePendingIds(FIXED)).thenReturn(List.of(commitmentId));
    doThrow(new RuntimeException("row boom"))
        .when(missEnforcer)
        .markMissedAndEnqueue(commitmentId, FIXED);

    job.scheduledCycle();

    verify(sweepHealthState).markSuccess();
    verify(sweepHealthState, never()).markFailure(any());
    assertThat(appender.list)
        .anyMatch(
            event ->
                event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().contains(commitmentId.toString()));
  }
}

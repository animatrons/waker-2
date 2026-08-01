package com.waker.penalty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.waker.penalty.LeaderboardPenaltyConfig;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderboardPenaltyHandlerTest {

  private static final UUID COMMITMENT_ID = UUID.randomUUID();
  private static final Instant FIXED_NOW = Instant.parse("2026-07-31T06:15:00Z");
  private static final Clock CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
  private static final LeaderboardPenaltyConfig CONSENT_TRUE = new LeaderboardPenaltyConfig(true);
  private static final PenaltyDispatchContext CONTEXT =
      new PenaltyDispatchContext("Morning workout", "Amin Example");

  @Mock private LeaderboardEntryRepository repository;

  private LeaderboardPenaltyHandler handler;

  @BeforeEach
  void setUp() {
    handler = new LeaderboardPenaltyHandler(repository, CLOCK);
  }

  @Test
  void dispatchPersistsEntryWithWhoWhatWhenAndReturnsSuccess() {
    when(repository.save(any(LeaderboardEntry.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    PenaltyDispatchResult result = handler.dispatch(COMMITMENT_ID, CONSENT_TRUE, CONTEXT);

    assertThat(result.success()).isTrue();
    assertThat(result.detail()).contains("leaderboard entry recorded");

    ArgumentCaptor<LeaderboardEntry> captor = ArgumentCaptor.forClass(LeaderboardEntry.class);
    verify(repository).save(captor.capture());
    LeaderboardEntry saved = captor.getValue();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCommitmentId()).isEqualTo(COMMITMENT_ID);
    assertThat(saved.getUserDisplayName()).isEqualTo("Amin Example");
    assertThat(saved.getCommitmentName()).isEqualTo("Morning workout");
    assertThat(saved.getMissedAt()).isEqualTo(FIXED_NOW);
  }

  @Test
  void dispatchRefusesWhenConsentFalse() {
    PenaltyDispatchResult result =
        handler.dispatch(COMMITMENT_ID, new LeaderboardPenaltyConfig(false), CONTEXT);

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).containsIgnoringCase("consent");
    verify(repository, never()).save(any());
  }

  @Test
  void dispatchRefusesWhenConsentNull() {
    PenaltyDispatchResult result =
        handler.dispatch(COMMITMENT_ID, new LeaderboardPenaltyConfig(null), CONTEXT);

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).containsIgnoringCase("consent");
    verify(repository, never()).save(any());
  }

  @Test
  void dispatchRefusesWhenUserDisplayNameBlank() {
    PenaltyDispatchResult result =
        handler.dispatch(
            COMMITMENT_ID, CONSENT_TRUE, new PenaltyDispatchContext("Morning workout", "  "));

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).containsIgnoringCase("display context");
    verify(repository, never()).save(any());
  }

  @Test
  void dispatchRefusesWhenCommitmentNameBlank() {
    PenaltyDispatchResult result =
        handler.dispatch(
            COMMITMENT_ID, CONSENT_TRUE, new PenaltyDispatchContext(null, "Amin Example"));

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).containsIgnoringCase("display context");
    verify(repository, never()).save(any());
  }
}

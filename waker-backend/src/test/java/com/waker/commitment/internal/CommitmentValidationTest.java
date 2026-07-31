package com.waker.commitment.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.commitment.CommitmentProperties;
import com.waker.commitment.CommitmentValidationException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommitmentValidationTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-07-30T12:00:00Z");

  private CommitmentCreateValidator validator;

  @BeforeEach
  void setUp() {
    validator =
        new CommitmentCreateValidator(new CommitmentProperties(Duration.ofMinutes(10), 5, 100));
  }

  @Test
  void acceptsValidTimingWithinRules() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofHours(1));
    Instant deadline = FIXED_NOW.plus(Duration.ofHours(2));

    assertThatCode(() -> validator.validateTiming(FIXED_NOW, notifyTime, deadline))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsNotifyTimeLessThanFiveMinutesAhead() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofMinutes(4));
    Instant deadline = FIXED_NOW.plus(Duration.ofHours(1));

    assertThatThrownBy(() -> validator.validateTiming(FIXED_NOW, notifyTime, deadline))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("5 minutes");
  }

  @Test
  void rejectsNotifyTimeMoreThanTwentyFourHoursAhead() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofHours(24).plusMinutes(1));
    Instant deadline = notifyTime.plus(Duration.ofMinutes(30));

    assertThatThrownBy(() -> validator.validateTiming(FIXED_NOW, notifyTime, deadline))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("24 hours");
  }

  @Test
  void rejectsDeadlineMoreThanTwentyFourHoursAhead() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofHours(2));
    Instant deadline = FIXED_NOW.plus(Duration.ofHours(25));

    assertThatThrownBy(() -> validator.validateTiming(FIXED_NOW, notifyTime, deadline))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("deadline");
  }

  @Test
  void rejectsNotifyTimeNotBeforeDeadline() {
    Instant sameTime = FIXED_NOW.plus(Duration.ofHours(1));

    assertThatThrownBy(() -> validator.validateTiming(FIXED_NOW, sameTime, sameTime))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("strictly before deadline");
  }

  @Test
  void deadlineAfterEditWindowEndWhenNotifyIsAfterCooldown() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofMinutes(20));
    Instant deadline = FIXED_NOW.plus(Duration.ofMinutes(25));
    Instant editWindowEnd = FIXED_NOW.plus(Duration.ofMinutes(10));

    assertThat(validator.editWindowEnd(FIXED_NOW, notifyTime)).isEqualTo(editWindowEnd);
    assertThatCode(() -> validator.validateTiming(FIXED_NOW, notifyTime, deadline))
        .doesNotThrowAnyException();
  }

  @Test
  void editWindowEndUsesEarlierOfCooldownAndNotifyTime() {
    Instant notifyTime = FIXED_NOW.plus(Duration.ofMinutes(30));
    Instant cooldownEnd = FIXED_NOW.plus(Duration.ofMinutes(10));

    assertThat(validator.editWindowEnd(FIXED_NOW, notifyTime)).isEqualTo(cooldownEnd);
  }

  @Test
  void validateTimingForEditAnchorsHorizonToCreatedAt() {
    Instant createdAt = FIXED_NOW;
    Instant notifyTime = createdAt.plus(Duration.ofHours(1));
    Instant deadline = createdAt.plus(Duration.ofHours(2));

    assertThatCode(() -> validator.validateTimingForEdit(createdAt, notifyTime, deadline))
        .doesNotThrowAnyException();
  }

  @Test
  void validateTimingForEditRejectsDeadlineBeyondTwentyFourHoursFromCreation() {
    Instant createdAt = FIXED_NOW;
    Instant notifyTime = createdAt.plus(Duration.ofHours(2));
    Instant deadline = createdAt.plus(Duration.ofHours(25));

    assertThatThrownBy(() -> validator.validateTimingForEdit(createdAt, notifyTime, deadline))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("deadline");
  }

  @Test
  void validateTimingForEditRejectsNotifyTimeBeforeCreatedAtPlusFiveMinutes() {
    Instant createdAt = FIXED_NOW;
    Instant notifyTime = createdAt.plus(Duration.ofMinutes(4));
    Instant deadline = createdAt.plus(Duration.ofHours(1));

    assertThatThrownBy(() -> validator.validateTimingForEdit(createdAt, notifyTime, deadline))
        .isInstanceOf(CommitmentValidationException.class)
        .hasMessageContaining("5 minutes");
  }
}

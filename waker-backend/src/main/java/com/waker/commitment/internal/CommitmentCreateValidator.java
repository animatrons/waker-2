package com.waker.commitment.internal;

import com.waker.commitment.CommitmentProperties;
import com.waker.commitment.CommitmentValidationException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class CommitmentCreateValidator {

  private static final Duration MIN_NOTIFY_LEAD = Duration.ofMinutes(5);
  private static final Duration MAX_HORIZON = Duration.ofHours(24);

  private final Duration editWindowCooldown;

  CommitmentCreateValidator(CommitmentProperties properties) {
    this.editWindowCooldown = properties.editWindowCooldown();
  }

  void validateTiming(Instant now, Instant notifyTime, Instant deadline) {
    validateTimingAnchored(now, notifyTime, deadline);
  }

  void validateTimingForEdit(Instant createdAt, Instant notifyTime, Instant deadline) {
    validateTimingAnchored(createdAt, notifyTime, deadline);
  }

  Instant editWindowEnd(Instant createdAt, Instant notifyTime) {
    Instant cooldownEnd = createdAt.plus(editWindowCooldown);
    return cooldownEnd.isBefore(notifyTime) ? cooldownEnd : notifyTime;
  }

  private void validateTimingAnchored(Instant anchor, Instant notifyTime, Instant deadline) {
    Instant maxHorizon = anchor.plus(MAX_HORIZON);

    if (notifyTime.isBefore(anchor.plus(MIN_NOTIFY_LEAD))) {
      throw new CommitmentValidationException(
          "notifyTime must be at least 5 minutes ahead of creation");
    }
    if (notifyTime.isAfter(maxHorizon)) {
      throw new CommitmentValidationException("notifyTime must not be more than 24 hours ahead");
    }
    if (deadline.isAfter(maxHorizon)) {
      throw new CommitmentValidationException("deadline must not be more than 24 hours ahead");
    }
    if (!notifyTime.isBefore(deadline)) {
      throw new CommitmentValidationException("notifyTime must be strictly before deadline");
    }

    Instant editWindowEnd = editWindowEnd(anchor, notifyTime);
    if (deadline.isBefore(editWindowEnd)) {
      throw new CommitmentValidationException(
          "deadline must not fall before the edit window closes");
    }
  }
}

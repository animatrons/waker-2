package com.waker.commitment.internal;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Thread-safe last-success / last-error signal for the commitment sweep cycle (FR-19). */
@Component
class SweepHealthState {

  private final Clock clock;
  private final AtomicReference<Instant> lastSuccessfulRun = new AtomicReference<>();
  private final AtomicReference<String> lastErrorMessage = new AtomicReference<>();

  SweepHealthState(@Qualifier("commitmentClock") Clock clock) {
    this.clock = clock;
  }

  void markSuccess() {
    lastSuccessfulRun.set(Instant.now(clock));
    lastErrorMessage.set(null);
  }

  void markFailure(Throwable error) {
    lastErrorMessage.set(error == null ? "unknown" : error.toString());
  }

  Optional<Instant> lastSuccessfulRun() {
    return Optional.ofNullable(lastSuccessfulRun.get());
  }

  Optional<String> lastErrorMessage() {
    return Optional.ofNullable(lastErrorMessage.get());
  }

  /** Test-only reset so singleton state does not leak across IT methods. */
  void clearForTests() {
    lastSuccessfulRun.set(null);
    lastErrorMessage.set(null);
  }
}

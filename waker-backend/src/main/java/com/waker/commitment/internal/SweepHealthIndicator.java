package com.waker.commitment.internal;

import java.time.Instant;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator component {@code sweep} exposing last successful sweep run (FR-19 / AD-9).
 *
 * <p>Stays {@code UP} (or {@code UP} with {@code "never"}) so a quiet scheduler does not fail
 * readiness probes — Amin watches whether the timestamp advances across ticks.
 */
@Component("sweep")
class SweepHealthIndicator implements HealthIndicator {

  private final SweepHealthState state;

  SweepHealthIndicator(SweepHealthState state) {
    this.state = state;
  }

  @Override
  public Health health() {
    Instant last = state.lastSuccessfulRun().orElse(null);
    Health.Builder builder =
        Health.up().withDetail("lastSuccessfulSweepRun", last == null ? "never" : last.toString());
    state.lastErrorMessage().ifPresent(msg -> builder.withDetail("lastErrorMessage", msg));
    return builder.build();
  }
}

package com.waker.commitment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.sweep")
public record SweepProperties(Duration interval, int dispatchBatchSize) {

  public SweepProperties {
    Assert.notNull(interval, "waker.sweep.interval must not be null");
    Assert.isTrue(
        !interval.isZero() && !interval.isNegative(), "waker.sweep.interval must be positive");
    Assert.isTrue(dispatchBatchSize > 0, "waker.sweep.dispatch-batch-size must be positive");
  }
}

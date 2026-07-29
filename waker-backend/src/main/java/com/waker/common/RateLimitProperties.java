package com.waker.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.rate-limit")
public record RateLimitProperties(long capacity, Duration window) {

  public RateLimitProperties {
    Assert.isTrue(capacity > 0, "waker.rate-limit.capacity must be positive");
    Assert.notNull(window, "waker.rate-limit.window must not be null");
    Assert.isTrue(
        !window.isZero() && !window.isNegative(),
        "waker.rate-limit.window must be a positive duration");
  }
}

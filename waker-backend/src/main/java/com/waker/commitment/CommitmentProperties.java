package com.waker.commitment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.commitment")
public record CommitmentProperties(Duration editWindowCooldown, int maxPending) {

  public CommitmentProperties {
    Assert.notNull(editWindowCooldown, "waker.commitment.edit-window-cooldown must not be null");
    Assert.isTrue(
        !editWindowCooldown.isZero() && !editWindowCooldown.isNegative(),
        "waker.commitment.edit-window-cooldown must be positive");
    Assert.isTrue(maxPending > 0, "waker.commitment.max-pending must be positive");
  }
}

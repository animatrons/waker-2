package com.waker.penalty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.penalty")
public record PenaltyProperties(int maxPageSize) {

  public PenaltyProperties {
    Assert.isTrue(maxPageSize > 0, "waker.penalty.max-page-size must be positive");
  }
}

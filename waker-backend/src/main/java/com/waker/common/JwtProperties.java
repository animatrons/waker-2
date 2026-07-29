package com.waker.common;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.jwt")
public record JwtProperties(String secret, Duration expiration) {

  private static final int MIN_SECRET_LENGTH = 32;

  public JwtProperties {
    Assert.hasText(secret, "waker.jwt.secret must not be blank");
    Assert.isTrue(
        secret.length() >= MIN_SECRET_LENGTH,
        "waker.jwt.secret must be at least "
            + MIN_SECRET_LENGTH
            + " characters (256-bit HS256 key material)");
    Assert.notNull(expiration, "waker.jwt.expiration must not be null");
    Assert.isTrue(
        !expiration.isZero() && !expiration.isNegative(),
        "waker.jwt.expiration must be a positive duration");
  }
}

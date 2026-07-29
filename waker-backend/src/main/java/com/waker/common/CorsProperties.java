package com.waker.common;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.cors")
public record CorsProperties(List<String> allowedOrigins) {

  public CorsProperties {
    Assert.notEmpty(allowedOrigins, "waker.cors.allowed-origins must not be empty");
    Assert.isTrue(
        allowedOrigins.stream().noneMatch("*"::equals),
        "waker.cors.allowed-origins must never contain '*' (AD-9)");
  }
}

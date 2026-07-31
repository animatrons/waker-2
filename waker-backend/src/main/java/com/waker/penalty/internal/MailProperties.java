package com.waker.penalty.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "waker.mail")
record MailProperties(String from) {

  MailProperties {
    Assert.hasText(from, "waker.mail.from must not be blank");
  }
}

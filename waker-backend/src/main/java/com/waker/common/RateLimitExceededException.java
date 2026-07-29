package com.waker.common;

/** Thrown when an auth rate-limit bucket is exhausted (IP or account). */
public class RateLimitExceededException extends RuntimeException {

  private final Long retryAfterSeconds;

  public RateLimitExceededException() {
    this(null);
  }

  public RateLimitExceededException(Long retryAfterSeconds) {
    super("Rate limit exceeded. Try again later.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
}

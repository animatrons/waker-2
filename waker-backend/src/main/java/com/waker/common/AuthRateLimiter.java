package com.waker.common;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * In-memory Bucket4j throttle for auth endpoints (single Compose instance). Keys are opaque strings
 * such as {@code ip:...} or {@code email:...}. Client IP uses {@code request.getRemoteAddr()} only
 * — trusted proxy / {@code X-Forwarded-For} is out of scope until a reverse proxy is configured.
 */
@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class AuthRateLimiter {

  private final RateLimitProperties properties;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  public AuthRateLimiter(RateLimitProperties properties) {
    this.properties = properties;
  }

  /**
   * Consumes one token for {@code key}. Throws {@link RateLimitExceededException} when the bucket
   * is empty.
   */
  public void checkOrThrow(String key) {
    Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket());
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      return;
    }
    long nanos = probe.getNanosToWaitForRefill();
    long seconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(nanos));
    throw new RateLimitExceededException(seconds);
  }

  /** Test helper — clears all in-memory buckets. */
  public void reset() {
    buckets.clear();
  }

  private Bucket newBucket() {
    Bandwidth limit =
        Bandwidth.builder()
            .capacity(properties.capacity())
            .refillGreedy(properties.capacity(), properties.window())
            .build();
    return Bucket.builder().addLimit(limit).build();
  }
}

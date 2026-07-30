package com.waker.mission;

import java.util.UUID;

public interface MissionHandler {

  MissionType missionType();

  /** Create-time validation of config shape (Story 2.3 will call via {@link MissionDispatch}). */
  void validateConfig(MissionConfig config);

  /**
   * Enrich config for persistence — e.g. server-generated math problem (FR-11). Default: identity.
   */
  default MissionConfig prepareForPersist(MissionConfig config) {
    return config;
  }

  /**
   * Fulfillment verification — implemented in Stories 2.6/2.7.
   *
   * <p>Signature uses UUID + value types only — never {@code Commitment} or {@code User}.
   */
  MissionVerificationResult verifyFulfillment(
      UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof);
}

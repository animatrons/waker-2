package com.waker.penalty;

import java.util.UUID;

public interface PenaltyHandler {

  PenaltyType penaltyType();

  /** Create-time validation — Story 2.3 calls via {@link PenaltyDispatch} on POST /commitments. */
  void validateConfig(PenaltyConfig config);

  /**
   * Side-effect delivery — implemented in Epic 3 (Stories 3.2/3.3).
   *
   * <p>Never accept Commitment/User entity types — use UUID + {@link PenaltyDispatchContext}
   * instead.
   */
  PenaltyDispatchResult dispatch(
      UUID commitmentId, PenaltyConfig config, PenaltyDispatchContext context);
}

package com.waker.penalty;

public record PenaltyDispatchResult(boolean success, String detail) {

  public static PenaltyDispatchResult success(String detail) {
    return new PenaltyDispatchResult(true, detail);
  }

  public static PenaltyDispatchResult failure(String detail) {
    return new PenaltyDispatchResult(false, detail);
  }
}

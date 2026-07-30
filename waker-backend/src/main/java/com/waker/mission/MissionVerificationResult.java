package com.waker.mission;

public record MissionVerificationResult(boolean accepted, String rejectionReason) {

  public static MissionVerificationResult success() {
    return new MissionVerificationResult(true, null);
  }

  public static MissionVerificationResult rejected(String reason) {
    return new MissionVerificationResult(false, reason);
  }
}

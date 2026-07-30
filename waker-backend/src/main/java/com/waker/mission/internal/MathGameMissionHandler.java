package com.waker.mission.internal;

import com.waker.mission.MathGameMissionConfig;
import com.waker.mission.MissionConfig;
import com.waker.mission.MissionFulfillmentProof;
import com.waker.mission.MissionHandler;
import com.waker.mission.MissionType;
import com.waker.mission.MissionVerificationResult;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class MathGameMissionHandler implements MissionHandler {

  @Override
  public MissionType missionType() {
    return MissionType.MATH_GAME;
  }

  @Override
  public void validateConfig(MissionConfig config) {
    if (!(config instanceof MathGameMissionConfig math)) {
      throw new IllegalArgumentException("Expected MathGameMissionConfig for MATH_GAME");
    }
    if (math.problemStatement() == null || math.problemStatement().isBlank()) {
      throw new IllegalArgumentException("problemStatement must not be blank");
    }
    if (math.expectedAnswer() == null || math.expectedAnswer().isBlank()) {
      throw new IllegalArgumentException("expectedAnswer must not be blank");
    }
  }

  @Override
  public MissionVerificationResult verifyFulfillment(
      UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof) {
    throw new UnsupportedOperationException(
        "Math game fulfillment verification is not implemented yet");
  }
}

package com.waker.mission.internal;

import com.waker.mission.MathGameMissionConfig;
import com.waker.mission.MissionConfig;
import com.waker.mission.MissionFulfillmentProof;
import com.waker.mission.MissionHandler;
import com.waker.mission.MissionType;
import com.waker.mission.MissionVerificationResult;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
class MathGameMissionHandler implements MissionHandler {

  @Override
  public MissionType missionType() {
    return MissionType.MATH_GAME;
  }

  @Override
  public void validateConfig(MissionConfig config) {
    if (!(config instanceof MathGameMissionConfig)) {
      throw new IllegalArgumentException("Expected MathGameMissionConfig for MATH_GAME");
    }
  }

  @Override
  public MissionConfig prepareForPersist(MissionConfig config) {
    if (!(config instanceof MathGameMissionConfig)) {
      throw new IllegalArgumentException("Expected MathGameMissionConfig for MATH_GAME");
    }
    int left = ThreadLocalRandom.current().nextInt(1, 10);
    int right = ThreadLocalRandom.current().nextInt(1, 10);
    int sum = left + right;
    return new MathGameMissionConfig(left + " + " + right, String.valueOf(sum));
  }

  @Override
  public MissionVerificationResult verifyFulfillment(
      UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof) {
    throw new UnsupportedOperationException(
        "Math game fulfillment verification is not implemented yet");
  }
}

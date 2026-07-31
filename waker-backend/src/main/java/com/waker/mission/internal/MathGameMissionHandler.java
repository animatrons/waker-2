package com.waker.mission.internal;

import com.waker.mission.MathGameFulfillmentProof;
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
    if (!(config instanceof MathGameMissionConfig math)) {
      throw new IllegalArgumentException("Expected MathGameMissionConfig for MATH_GAME");
    }
    if (!(proof instanceof MathGameFulfillmentProof mathProof)) {
      throw new IllegalArgumentException("Expected MathGameFulfillmentProof for MATH_GAME");
    }

    if (mathProof.submittedAnswer() == null || mathProof.submittedAnswer().isBlank()) {
      return MissionVerificationResult.rejected("submitted answer must not be blank");
    }
    if (!math.expectedAnswer().equals(mathProof.submittedAnswer().trim())) {
      return MissionVerificationResult.rejected("incorrect answer");
    }
    return MissionVerificationResult.success();
  }
}

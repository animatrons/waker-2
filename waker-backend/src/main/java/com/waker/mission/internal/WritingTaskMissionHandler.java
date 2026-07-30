package com.waker.mission.internal;

import com.waker.mission.MissionConfig;
import com.waker.mission.MissionFulfillmentProof;
import com.waker.mission.MissionHandler;
import com.waker.mission.MissionType;
import com.waker.mission.MissionVerificationResult;
import com.waker.mission.WritingTaskMissionConfig;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class WritingTaskMissionHandler implements MissionHandler {

  @Override
  public MissionType missionType() {
    return MissionType.WRITING_TASK;
  }

  @Override
  public void validateConfig(MissionConfig config) {
    if (!(config instanceof WritingTaskMissionConfig writing)) {
      throw new IllegalArgumentException("Expected WritingTaskMissionConfig for WRITING_TASK");
    }
    if (writing.prompt() == null || writing.prompt().isBlank()) {
      throw new IllegalArgumentException("prompt must not be blank");
    }
    if (writing.prompt().length() > 500) {
      throw new IllegalArgumentException("prompt must not exceed 500 characters");
    }
    if (writing.minimumLength() < 1 || writing.minimumLength() > 10_000) {
      throw new IllegalArgumentException("minimumLength must be between 1 and 10000");
    }
  }

  @Override
  public MissionVerificationResult verifyFulfillment(
      UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof) {
    throw new UnsupportedOperationException(
        "Writing task fulfillment verification is not implemented yet");
  }
}

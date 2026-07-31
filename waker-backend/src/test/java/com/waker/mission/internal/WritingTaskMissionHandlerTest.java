package com.waker.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.mission.MissionVerificationResult;
import com.waker.mission.WritingTaskFulfillmentProof;
import com.waker.mission.WritingTaskMissionConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WritingTaskMissionHandlerTest {

  private static final UUID COMMITMENT_ID = UUID.randomUUID();
  private static final WritingTaskMissionConfig CONFIG =
      new WritingTaskMissionConfig("Write why you will wake up on time", 10);

  private final WritingTaskMissionHandler handler = new WritingTaskMissionHandler();

  @Test
  void acceptsTextAtMinimumLength() {
    MissionVerificationResult result =
        handler.verifyFulfillment(
            COMMITMENT_ID, CONFIG, new WritingTaskFulfillmentProof("1234567890"));

    assertThat(result.accepted()).isTrue();
  }

  @Test
  void rejectsTextBelowMinimumLength() {
    MissionVerificationResult result =
        handler.verifyFulfillment(
            COMMITMENT_ID, CONFIG, new WritingTaskFulfillmentProof("123456789"));

    assertThat(result.accepted()).isFalse();
    assertThat(result.rejectionReason()).contains("below minimum");
  }

  @Test
  void acceptsTextAtMaximumLength() {
    String text = "x".repeat(10_000);

    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new WritingTaskFulfillmentProof(text));

    assertThat(result.accepted()).isTrue();
  }

  @Test
  void rejectsTextAboveMaximumLength() {
    String text = "x".repeat(10_001);

    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new WritingTaskFulfillmentProof(text));

    assertThat(result.accepted()).isFalse();
    assertThat(result.rejectionReason()).contains("maximum length");
  }

  @Test
  void rejectsBlankText() {
    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new WritingTaskFulfillmentProof("   "));

    assertThat(result.accepted()).isFalse();
    assertThat(result.rejectionReason()).contains("blank");
  }
}

package com.waker.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.mission.MathGameFulfillmentProof;
import com.waker.mission.MathGameMissionConfig;
import com.waker.mission.MissionVerificationResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MathGameMissionHandlerTest {

  private static final UUID COMMITMENT_ID = UUID.randomUUID();
  private static final MathGameMissionConfig CONFIG = new MathGameMissionConfig("3 + 4", "7");

  private final MathGameMissionHandler handler = new MathGameMissionHandler();

  @Test
  void acceptsCorrectAnswer() {
    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new MathGameFulfillmentProof("7"));

    assertThat(result.accepted()).isTrue();
  }

  @Test
  void acceptsWhitespacePaddedCorrectAnswer() {
    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new MathGameFulfillmentProof("  7  "));

    assertThat(result.accepted()).isTrue();
  }

  @Test
  void rejectsIncorrectAnswer() {
    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new MathGameFulfillmentProof("8"));

    assertThat(result.accepted()).isFalse();
    assertThat(result.rejectionReason()).isEqualTo("incorrect answer");
  }

  @Test
  void rejectsBlankAnswer() {
    MissionVerificationResult result =
        handler.verifyFulfillment(COMMITMENT_ID, CONFIG, new MathGameFulfillmentProof("   "));

    assertThat(result.accepted()).isFalse();
    assertThat(result.rejectionReason()).contains("blank");
  }
}

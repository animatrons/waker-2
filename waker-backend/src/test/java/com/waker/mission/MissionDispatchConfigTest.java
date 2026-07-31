package com.waker.mission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MissionDispatchConfigTest {

  @Test
  void buildsDispatchWhenAllHandlersPresent() {
    MissionDispatch dispatch =
        MissionDispatch.fromHandlers(
            List.of(
                stubHandler(MissionType.QR_CODE),
                stubHandler(MissionType.WRITING_TASK),
                stubHandler(MissionType.MATH_GAME)));

    assertThat(dispatch.handlerFor(MissionType.QR_CODE)).isNotNull();
    assertThat(dispatch.handlerFor(MissionType.WRITING_TASK)).isNotNull();
    assertThat(dispatch.handlerFor(MissionType.MATH_GAME)).isNotNull();
  }

  @Test
  void failsWhenHandlerMissingForMissionType() {
    assertThatThrownBy(
            () ->
                MissionDispatch.fromHandlers(
                    List.of(
                        stubHandler(MissionType.QR_CODE), stubHandler(MissionType.WRITING_TASK))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing MissionHandler for MATH_GAME");
  }

  @Test
  void failsWhenDuplicateHandlerRegistered() {
    assertThatThrownBy(
            () ->
                MissionDispatch.fromHandlers(
                    List.of(
                        stubHandler(MissionType.QR_CODE),
                        stubHandler(MissionType.QR_CODE),
                        stubHandler(MissionType.WRITING_TASK),
                        stubHandler(MissionType.MATH_GAME))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate MissionHandler for QR_CODE");
  }

  @Test
  void verifyFulfillmentRejectsWritingConfigWithQrProof() {
    MissionDispatch dispatch =
        MissionDispatch.fromHandlers(
            List.of(
                stubHandler(MissionType.QR_CODE),
                stubHandler(MissionType.WRITING_TASK),
                stubHandler(MissionType.MATH_GAME)));

    assertThatThrownBy(
            () ->
                dispatch.verifyFulfillment(
                    UUID.randomUUID(),
                    new WritingTaskMissionConfig("prompt", 10),
                    new QrCodeFulfillmentProof("payload")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
  }

  @Test
  void verifyFulfillmentRejectsMathConfigWithWritingProof() {
    MissionDispatch dispatch =
        MissionDispatch.fromHandlers(
            List.of(
                stubHandler(MissionType.QR_CODE),
                stubHandler(MissionType.WRITING_TASK),
                stubHandler(MissionType.MATH_GAME)));

    assertThatThrownBy(
            () ->
                dispatch.verifyFulfillment(
                    UUID.randomUUID(),
                    new MathGameMissionConfig("3 + 4", "7"),
                    new WritingTaskFulfillmentProof("enough text here")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
  }

  private static MissionHandler stubHandler(MissionType type) {
    return new MissionHandler() {
      @Override
      public MissionType missionType() {
        return type;
      }

      @Override
      public void validateConfig(MissionConfig config) {}

      @Override
      public MissionVerificationResult verifyFulfillment(
          UUID commitmentId, MissionConfig config, MissionFulfillmentProof proof) {
        throw new UnsupportedOperationException("stub");
      }
    };
  }
}

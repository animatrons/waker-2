package com.waker.mission;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MissionHandlerValidationTest extends AbstractIntegrationTest {

  @Autowired private MissionDispatch missionDispatch;

  @Test
  void rejectsBlankQrPayload() {
    assertThatThrownBy(() -> missionDispatch.validateConfig(new QrCodeMissionConfig("   ")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("codePayload");
  }

  @Test
  void rejectsOversizedQrPayload() {
    assertThatThrownBy(
            () -> missionDispatch.validateConfig(new QrCodeMissionConfig("x".repeat(513))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("512");
  }

  @Test
  void rejectsInvalidWritingTaskMinimumLength() {
    assertThatThrownBy(
            () -> missionDispatch.validateConfig(new WritingTaskMissionConfig("prompt", 0)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("minimumLength");
  }

  @Test
  void acceptsMathGameWireConfigWithoutClientFields() {
    assertThatCode(() -> missionDispatch.validateConfig(new MathGameMissionConfig(null, null)))
        .doesNotThrowAnyException();
  }
}

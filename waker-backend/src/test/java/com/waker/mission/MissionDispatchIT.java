package com.waker.mission;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MissionDispatchIT extends AbstractIntegrationTest {

  @Autowired private MissionDispatch missionDispatch;

  @Test
  void contextLoadsWithAllMissionHandlersRegistered() {
    assertThat(missionDispatch.handlerFor(MissionType.QR_CODE)).isNotNull();
    assertThat(missionDispatch.handlerFor(MissionType.WRITING_TASK)).isNotNull();
    assertThat(missionDispatch.handlerFor(MissionType.MATH_GAME)).isNotNull();
  }
}

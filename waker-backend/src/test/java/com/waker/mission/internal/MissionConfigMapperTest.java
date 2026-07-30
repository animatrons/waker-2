package com.waker.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import com.waker.mission.MathGameMissionConfig;
import com.waker.mission.QrCodeMissionConfig;
import com.waker.mission.WritingTaskMissionConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MissionConfigMapperTest extends AbstractIntegrationTest {

  @Autowired private MissionConfigMapper missionConfigMapper;

  @Test
  void copiesEachMissionConfigSubtype() {
    var qr = new QrCodeMissionConfig("payload");
    var writing = new WritingTaskMissionConfig("prompt", 10);
    var math = new MathGameMissionConfig("1 + 1", "2");

    assertThat(missionConfigMapper.copy(qr)).isEqualTo(qr);
    assertThat(missionConfigMapper.copy(writing)).isEqualTo(writing);
    assertThat(missionConfigMapper.copy(math)).isEqualTo(math);
  }
}

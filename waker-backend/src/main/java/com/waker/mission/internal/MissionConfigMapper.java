package com.waker.mission.internal;

import com.waker.mission.MathGameMissionConfig;
import com.waker.mission.MissionConfig;
import com.waker.mission.QrCodeMissionConfig;
import com.waker.mission.WritingTaskMissionConfig;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = "spring")
public interface MissionConfigMapper {

  @SubclassMapping(source = QrCodeMissionConfig.class, target = QrCodeMissionConfig.class)
  @SubclassMapping(source = WritingTaskMissionConfig.class, target = WritingTaskMissionConfig.class)
  @SubclassMapping(source = MathGameMissionConfig.class, target = MathGameMissionConfig.class)
  MissionConfig copy(MissionConfig config);
}

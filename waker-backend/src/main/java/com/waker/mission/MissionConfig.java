package com.waker.mission;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
  @JsonSubTypes.Type(value = QrCodeMissionConfig.class, name = "QR_CODE"),
  @JsonSubTypes.Type(value = WritingTaskMissionConfig.class, name = "WRITING_TASK"),
  @JsonSubTypes.Type(value = MathGameMissionConfig.class, name = "MATH_GAME")
})
public sealed interface MissionConfig
    permits QrCodeMissionConfig, WritingTaskMissionConfig, MathGameMissionConfig {}

package com.waker.mission;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_class")
@JsonSubTypes({
  @JsonSubTypes.Type(value = QrCodeFulfillmentProof.class, name = "QR_CODE"),
  @JsonSubTypes.Type(value = WritingTaskFulfillmentProof.class, name = "WRITING_TASK"),
  @JsonSubTypes.Type(value = MathGameFulfillmentProof.class, name = "MATH_GAME")
})
public sealed interface MissionFulfillmentProof
    permits QrCodeFulfillmentProof, WritingTaskFulfillmentProof, MathGameFulfillmentProof {}

package com.waker.mission;

public sealed interface MissionFulfillmentProof
    permits QrCodeFulfillmentProof, WritingTaskFulfillmentProof, MathGameFulfillmentProof {}

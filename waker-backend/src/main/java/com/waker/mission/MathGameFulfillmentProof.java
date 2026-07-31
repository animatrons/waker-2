package com.waker.mission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MathGameFulfillmentProof(@NotBlank @Size(max = 32) String submittedAnswer)
    implements MissionFulfillmentProof {}

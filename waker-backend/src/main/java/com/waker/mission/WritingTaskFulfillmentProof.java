package com.waker.mission;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WritingTaskFulfillmentProof(@NotBlank @Size(max = 10_000) String submittedText)
    implements MissionFulfillmentProof {}

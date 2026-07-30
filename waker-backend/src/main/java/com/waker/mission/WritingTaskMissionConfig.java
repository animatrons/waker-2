package com.waker.mission;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WritingTaskMissionConfig(
    @NotBlank @Size(max = 500) String prompt, @Min(1) @Max(10_000) int minimumLength)
    implements MissionConfig {}

package com.waker.commitment;

import com.waker.mission.MissionConfig;
import com.waker.penalty.PenaltyConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpdateCommitmentRequest(
    @NotBlank @Size(min = 1, max = 200) String name,
    @Size(max = 1000) String description,
    @NotNull OffsetDateTime notifyTime,
    @NotNull OffsetDateTime deadline,
    @NotNull MissionConfig missionConfig,
    @NotNull PenaltyConfig penaltyConfig) {}

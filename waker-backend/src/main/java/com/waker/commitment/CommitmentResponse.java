package com.waker.commitment;

import com.waker.mission.MissionConfig;
import com.waker.penalty.PenaltyConfig;
import java.time.Instant;
import java.util.UUID;

public record CommitmentResponse(
    UUID id,
    String name,
    String description,
    CommitmentStatus status,
    Instant notifyTime,
    Instant deadline,
    MissionConfig missionConfig,
    PenaltyConfig penaltyConfig,
    Instant createdAt,
    Instant updatedAt) {}

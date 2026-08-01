package com.waker.penalty;

import java.time.Instant;
import java.util.UUID;

public record LeaderboardEntryResponse(
    UUID id, UUID commitmentId, String userDisplayName, String commitmentName, Instant missedAt) {}

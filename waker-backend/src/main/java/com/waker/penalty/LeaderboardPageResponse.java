package com.waker.penalty;

import java.util.List;

public record LeaderboardPageResponse(
    List<LeaderboardEntryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages) {}

package com.waker.commitment;

import java.util.List;

public record CommitmentPageResponse(
    List<CommitmentResponse> content, int page, int size, long totalElements, int totalPages) {}

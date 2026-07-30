package com.waker.commitment;

import java.util.UUID;

public interface CommitmentService {

  CommitmentResponse create(UUID ownerId, CreateCommitmentRequest request);
}

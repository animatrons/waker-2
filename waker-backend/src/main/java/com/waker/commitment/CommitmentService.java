package com.waker.commitment;

import java.util.UUID;

public interface CommitmentService {

  CommitmentResponse create(UUID ownerId, CreateCommitmentRequest request);

  CommitmentResponse update(UUID ownerId, UUID id, UpdateCommitmentRequest request);

  void cancel(UUID ownerId, UUID id);
}

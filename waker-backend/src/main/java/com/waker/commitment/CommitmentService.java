package com.waker.commitment;

import com.waker.mission.MissionFulfillmentProof;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface CommitmentService {

  CommitmentResponse create(UUID ownerId, CreateCommitmentRequest request);

  CommitmentResponse getById(UUID ownerId, UUID id);

  CommitmentPageResponse list(UUID ownerId, Optional<CommitmentStatus> status, Pageable pageable);

  CommitmentResponse update(UUID ownerId, UUID id, UpdateCommitmentRequest request);

  void cancel(UUID ownerId, UUID id);

  CommitmentResponse fulfill(UUID ownerId, UUID id, MissionFulfillmentProof proof);
}

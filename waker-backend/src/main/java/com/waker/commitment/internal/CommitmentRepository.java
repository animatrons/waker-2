package com.waker.commitment.internal;

import com.waker.commitment.CommitmentStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

  long countByUserIdAndStatus(UUID userId, CommitmentStatus status);
}

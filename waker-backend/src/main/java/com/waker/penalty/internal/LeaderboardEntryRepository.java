package com.waker.penalty.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, UUID> {}

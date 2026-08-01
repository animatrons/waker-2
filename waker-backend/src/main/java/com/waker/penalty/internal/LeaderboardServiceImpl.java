package com.waker.penalty.internal;

import com.waker.penalty.LeaderboardEntryResponse;
import com.waker.penalty.LeaderboardPageResponse;
import com.waker.penalty.LeaderboardService;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LeaderboardServiceImpl implements LeaderboardService {

  private final LeaderboardEntryRepository leaderboardEntryRepository;

  LeaderboardServiceImpl(LeaderboardEntryRepository leaderboardEntryRepository) {
    this.leaderboardEntryRepository = leaderboardEntryRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public LeaderboardPageResponse list(Pageable pageable) {
    Page<LeaderboardEntry> page = leaderboardEntryRepository.findAll(pageable);
    List<LeaderboardEntryResponse> content =
        page.getContent().stream().map(this::toResponse).toList();
    return new LeaderboardPageResponse(
        content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  private LeaderboardEntryResponse toResponse(LeaderboardEntry entry) {
    return new LeaderboardEntryResponse(
        entry.getId(),
        entry.getCommitmentId(),
        entry.getUserDisplayName(),
        entry.getCommitmentName(),
        entry.getMissedAt());
  }
}

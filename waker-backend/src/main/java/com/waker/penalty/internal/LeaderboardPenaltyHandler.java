package com.waker.penalty.internal;

import com.waker.penalty.InvalidPenaltyConfigException;
import com.waker.penalty.LeaderboardPenaltyConfig;
import com.waker.penalty.PenaltyConfig;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import com.waker.penalty.PenaltyHandler;
import com.waker.penalty.PenaltyType;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LeaderboardPenaltyHandler implements PenaltyHandler {

  private final LeaderboardEntryRepository leaderboardEntryRepository;
  private final Clock clock;

  LeaderboardPenaltyHandler(LeaderboardEntryRepository leaderboardEntryRepository, Clock clock) {
    this.leaderboardEntryRepository = leaderboardEntryRepository;
    this.clock = clock;
  }

  @Override
  public PenaltyType penaltyType() {
    return PenaltyType.LEADERBOARD;
  }

  @Override
  public void validateConfig(PenaltyConfig config) {
    if (!(config instanceof LeaderboardPenaltyConfig leaderboard)) {
      throw new InvalidPenaltyConfigException("Expected LeaderboardPenaltyConfig for LEADERBOARD");
    }
    if (leaderboard.consent() == null) {
      throw new InvalidPenaltyConfigException("consent must not be null");
    }
    if (!leaderboard.consent()) {
      throw new InvalidPenaltyConfigException("consent must be true at create time");
    }
  }

  @Override
  public PenaltyDispatchResult dispatch(
      UUID commitmentId, PenaltyConfig config, PenaltyDispatchContext context) {
    if (!(config instanceof LeaderboardPenaltyConfig leaderboard)) {
      throw new IllegalArgumentException("Expected LeaderboardPenaltyConfig for LEADERBOARD");
    }
    if (leaderboard.consent() == null || !leaderboard.consent()) {
      return PenaltyDispatchResult.failure("consent required — refuse publish");
    }
    if (context == null
        || isBlank(context.userDisplayName())
        || isBlank(context.commitmentName())) {
      return PenaltyDispatchResult.failure("missing display context for leaderboard");
    }

    LeaderboardEntry entry =
        new LeaderboardEntry(
            UUID.randomUUID(),
            commitmentId,
            context.userDisplayName().trim(),
            context.commitmentName().trim(),
            Instant.now(clock));
    leaderboardEntryRepository.save(entry);
    return PenaltyDispatchResult.success("leaderboard entry recorded");
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}

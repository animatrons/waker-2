package com.waker.penalty;

import org.springframework.data.domain.Pageable;

/**
 * Public facade for the shared wall-of-shame leaderboard (FR-15).
 *
 * <p>Returns a <strong>global</strong> list (all entries) — not filtered to the JWT user. The wall
 * of shame is intentionally shared for dogfood.
 */
public interface LeaderboardService {

  LeaderboardPageResponse list(Pageable pageable);
}

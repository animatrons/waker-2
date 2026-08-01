package com.waker.penalty;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated wall-of-shame read API (FR-15). Global list — not scoped to the JWT subject.
 *
 * <p>External public social posting is out of scope.
 */
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

  private final LeaderboardService leaderboardService;
  private final PenaltyProperties penaltyProperties;

  public LeaderboardController(
      LeaderboardService leaderboardService, PenaltyProperties penaltyProperties) {
    this.leaderboardService = leaderboardService;
    this.penaltyProperties = penaltyProperties;
  }

  @GetMapping
  public ResponseEntity<LeaderboardPageResponse> list(
      @PageableDefault(size = 20, sort = "missedAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    validatePageSize(pageable);
    return ResponseEntity.ok(leaderboardService.list(pageable));
  }

  private void validatePageSize(Pageable pageable) {
    if (pageable.getPageSize() > penaltyProperties.maxPageSize()) {
      throw new PenaltyValidationException(
          "size must not exceed " + penaltyProperties.maxPageSize());
    }
  }
}

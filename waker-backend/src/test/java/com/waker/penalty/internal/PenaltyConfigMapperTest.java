package com.waker.penalty.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.waker.AbstractIntegrationTest;
import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.LeaderboardPenaltyConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PenaltyConfigMapperTest extends AbstractIntegrationTest {

  @Autowired private PenaltyConfigMapper penaltyConfigMapper;

  @Test
  void copiesEachPenaltyConfigSubtype() {
    var email =
        new EmailToContactPenaltyConfig("friend@example.com", "I failed my wake-up commitment.");
    var leaderboard = new LeaderboardPenaltyConfig(true);

    assertThat(penaltyConfigMapper.copy(email)).isEqualTo(email);
    assertThat(penaltyConfigMapper.copy(leaderboard)).isEqualTo(leaderboard);
  }
}

package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PenaltyDispatchIT extends AbstractIntegrationTest {

  @Autowired private PenaltyDispatch penaltyDispatch;

  @Test
  void contextLoadsWithBothPenaltyHandlersRegistered() {
    assertThat(penaltyDispatch.handlerFor(PenaltyType.EMAIL_TO_CONTACT)).isNotNull();
    assertThat(penaltyDispatch.handlerFor(PenaltyType.LEADERBOARD)).isNotNull();
  }

  @Test
  void validateConfigAcceptsValidSamples() {
    assertThatCode(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("friend@example.com", "Wake up on time.")))
        .doesNotThrowAnyException();

    assertThatCode(() -> penaltyDispatch.validateConfig(new LeaderboardPenaltyConfig(true)))
        .doesNotThrowAnyException();
  }

  @Test
  void validateConfigRejectsInvalidSamples() {
    assertThatThrownBy(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("not-an-email", "Wake up on time.")))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("contactEmail");

    assertThatThrownBy(() -> penaltyDispatch.validateConfig(new LeaderboardPenaltyConfig(false)))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("consent");
  }
}

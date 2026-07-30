package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PenaltyConfigValidationTest extends AbstractIntegrationTest {

  @Autowired private PenaltyDispatch penaltyDispatch;

  @Test
  void acceptsValidEmailToContactConfig() {
    assertThatCode(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig(
                        "friend@example.com", "I failed my wake-up commitment again.")))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsBlankEmail() {
    assertThatThrownBy(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("   ", "Valid message")))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("contactEmail");
  }

  @Test
  void rejectsInvalidEmailFormat() {
    assertThatThrownBy(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("not-an-email", "Valid message")))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("contactEmail");
  }

  @Test
  void rejectsBlankMessage() {
    assertThatThrownBy(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("friend@example.com", "   ")))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("message");
  }

  @Test
  void rejectsOversizedMessage() {
    assertThatThrownBy(
            () ->
                penaltyDispatch.validateConfig(
                    new EmailToContactPenaltyConfig("friend@example.com", "x".repeat(2_001))))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("2000");
  }

  @Test
  void acceptsLeaderboardWithExplicitConsentTrue() {
    assertThatCode(() -> penaltyDispatch.validateConfig(new LeaderboardPenaltyConfig(true)))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsLeaderboardWithConsentFalse() {
    assertThatThrownBy(() -> penaltyDispatch.validateConfig(new LeaderboardPenaltyConfig(false)))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("consent");
  }

  @Test
  void rejectsLeaderboardWithNullConsent() {
    assertThatThrownBy(() -> penaltyDispatch.validateConfig(new LeaderboardPenaltyConfig(null)))
        .isInstanceOf(InvalidPenaltyConfigException.class)
        .hasMessageContaining("consent");
  }
}

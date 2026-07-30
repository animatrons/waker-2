package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PenaltyDispatchConfigTest {

  @Test
  void buildsDispatchWhenAllHandlersPresent() {
    PenaltyDispatch dispatch =
        PenaltyDispatch.fromHandlers(
            List.of(
                stubHandler(PenaltyType.EMAIL_TO_CONTACT), stubHandler(PenaltyType.LEADERBOARD)));

    assertThat(dispatch.handlerFor(PenaltyType.EMAIL_TO_CONTACT)).isNotNull();
    assertThat(dispatch.handlerFor(PenaltyType.LEADERBOARD)).isNotNull();
  }

  @Test
  void failsWhenHandlerMissingForPenaltyType() {
    assertThatThrownBy(
            () -> PenaltyDispatch.fromHandlers(List.of(stubHandler(PenaltyType.EMAIL_TO_CONTACT))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing PenaltyHandler for LEADERBOARD");
  }

  @Test
  void failsWhenDuplicateHandlerRegistered() {
    assertThatThrownBy(
            () ->
                PenaltyDispatch.fromHandlers(
                    List.of(
                        stubHandler(PenaltyType.EMAIL_TO_CONTACT),
                        stubHandler(PenaltyType.EMAIL_TO_CONTACT),
                        stubHandler(PenaltyType.LEADERBOARD))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Duplicate PenaltyHandler for EMAIL_TO_CONTACT");
  }

  private static PenaltyHandler stubHandler(PenaltyType type) {
    return new PenaltyHandler() {
      @Override
      public PenaltyType penaltyType() {
        return type;
      }

      @Override
      public void validateConfig(PenaltyConfig config) {}

      @Override
      public PenaltyDispatchResult dispatch(
          UUID commitmentId, PenaltyConfig config, PenaltyDispatchContext context) {
        throw new UnsupportedOperationException("stub");
      }
    };
  }
}

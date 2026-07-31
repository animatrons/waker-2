package com.waker.penalty.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

class EmailToContactPenaltyHandlerTest {

  private static final UUID COMMITMENT_ID = UUID.randomUUID();
  private static final EmailToContactPenaltyConfig CONFIG =
      new EmailToContactPenaltyConfig("friend@example.com", "You missed your commitment.");
  private static final PenaltyDispatchContext CONTEXT =
      new PenaltyDispatchContext("Morning run", "Amin");

  private SmtpEmailSender emailSender;
  private EmailToContactPenaltyHandler handler;

  @BeforeEach
  void setUp() {
    emailSender = mock(SmtpEmailSender.class);
    handler = new EmailToContactPenaltyHandler(emailSender);
  }

  @Test
  void dispatchSendsToContactWithSubjectAndMessage() {
    PenaltyDispatchResult result = handler.dispatch(COMMITMENT_ID, CONFIG, CONTEXT);

    assertThat(result.success()).isTrue();
    assertThat(result.detail()).contains("email accepted by SMTP");
    verify(emailSender)
        .sendPlainText(
            eq("friend@example.com"),
            eq("Waker commitment missed: Morning run"),
            eq("You missed your commitment.\n\nCommitment: Morning run"));
  }

  @Test
  void dispatchUsesFallbackSubjectWhenCommitmentNameBlank() {
    PenaltyDispatchContext blankName = new PenaltyDispatchContext("  ", "Amin");

    handler.dispatch(COMMITMENT_ID, CONFIG, blankName);

    verify(emailSender)
        .sendPlainText(
            eq("friend@example.com"),
            eq("Waker commitment missed"),
            eq("You missed your commitment."));
  }

  @Test
  void dispatchReturnsFailureOnMailException() {
    doThrow(new MailSendException("SMTP refused"))
        .when(emailSender)
        .sendPlainText(anyString(), anyString(), anyString());

    PenaltyDispatchResult result = handler.dispatch(COMMITMENT_ID, CONFIG, CONTEXT);

    assertThat(result.success()).isFalse();
    assertThat(result.detail()).contains("SMTP refused");
  }
}

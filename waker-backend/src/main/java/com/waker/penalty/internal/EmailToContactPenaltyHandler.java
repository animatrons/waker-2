package com.waker.penalty.internal;

import com.waker.penalty.EmailToContactPenaltyConfig;
import com.waker.penalty.InvalidPenaltyConfigException;
import com.waker.penalty.PenaltyConfig;
import com.waker.penalty.PenaltyDispatchContext;
import com.waker.penalty.PenaltyDispatchResult;
import com.waker.penalty.PenaltyHandler;
import com.waker.penalty.PenaltyType;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class EmailToContactPenaltyHandler implements PenaltyHandler {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

  @Override
  public PenaltyType penaltyType() {
    return PenaltyType.EMAIL_TO_CONTACT;
  }

  @Override
  public void validateConfig(PenaltyConfig config) {
    if (!(config instanceof EmailToContactPenaltyConfig email)) {
      throw new InvalidPenaltyConfigException(
          "Expected EmailToContactPenaltyConfig for EMAIL_TO_CONTACT");
    }
    if (email.contactEmail() == null || email.contactEmail().isBlank()) {
      throw new InvalidPenaltyConfigException("contactEmail must not be blank");
    }
    if (email.contactEmail().length() > 320) {
      throw new InvalidPenaltyConfigException("contactEmail must not exceed 320 characters");
    }
    if (!EMAIL_PATTERN.matcher(email.contactEmail()).matches()) {
      throw new InvalidPenaltyConfigException("contactEmail must be a valid email address");
    }
    if (email.message() == null || email.message().isBlank()) {
      throw new InvalidPenaltyConfigException("message must not be blank");
    }
    if (email.message().length() > 2_000) {
      throw new InvalidPenaltyConfigException("message must not exceed 2000 characters");
    }
  }

  @Override
  public PenaltyDispatchResult dispatch(
      UUID commitmentId, PenaltyConfig config, PenaltyDispatchContext context) {
    throw new UnsupportedOperationException("Email penalty dispatch is not implemented yet");
  }
}

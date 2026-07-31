package com.waker.penalty;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.waker.AbstractIntegrationTest;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class EmailToContactPenaltyDeliveryIT extends AbstractIntegrationTest {

  private static final GreenMail GREEN_MAIL = new GreenMail(ServerSetupTest.SMTP.dynamicPort());

  static {
    GREEN_MAIL.start();
  }

  @AfterAll
  static void stopMail() {
    GREEN_MAIL.stop();
  }

  @Autowired private PenaltyEventLedger penaltyEventLedger;
  @Autowired private PenaltyDispatch penaltyDispatch;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private JavaMailSenderImpl javaMailSender;

  @DynamicPropertySource
  static void registerMail(DynamicPropertyRegistry registry) {
    registry.add("spring.mail.host", () -> "127.0.0.1");
    registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getServerSetup().getPort());
    registry.add("spring.mail.username", () -> "");
    registry.add("spring.mail.password", () -> "");
    registry.add("spring.mail.properties.mail.smtp.auth", () -> "false");
    registry.add("spring.mail.properties.mail.smtp.starttls.enable", () -> "false");
    registry.add("waker.mail.from", () -> "waker@localhost");
  }

  @BeforeEach
  void resetMail() throws Exception {
    GREEN_MAIL.purgeEmailFromAllMailboxes();
    javaMailSender.setHost("127.0.0.1");
    javaMailSender.setPort(GREEN_MAIL.getSmtp().getServerSetup().getPort());
  }

  @Test
  void successfulDispatchMarksLedgerDispatchedAndCapturesMessage() throws Exception {
    UUID commitmentId = insertFixtureCommitment();
    EmailToContactPenaltyConfig config =
        new EmailToContactPenaltyConfig("friend@example.com", "You missed your morning run.");
    PenaltyDispatchContext context = new PenaltyDispatchContext("Morning run", "Amin");

    UUID eventId = penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);
    assertThat(penaltyEventLedger.claim(eventId)).isTrue();

    PenaltyDispatchResult result =
        penaltyDispatch
            .handlerFor(PenaltyType.EMAIL_TO_CONTACT)
            .dispatch(commitmentId, config, context);

    assertThat(result.success()).isTrue();
    assertThat(penaltyEventLedger.markDispatched(eventId)).isTrue();
    assertThat(outcome(eventId)).isEqualTo("DISPATCHED");

    assertThat(GREEN_MAIL.waitForIncomingEmail(5_000, 1)).isTrue();
    MimeMessage[] messages = GREEN_MAIL.getReceivedMessages();
    assertThat(messages).hasSize(1);
    assertThat(messages[0].getAllRecipients()[0].toString()).contains("friend@example.com");
    assertThat(messages[0].getSubject()).isEqualTo("Waker commitment missed: Morning run");
    assertThat(messages[0].getContent().toString()).contains("You missed your morning run.");
  }

  @Test
  void failedDispatchMarksLedgerFailed() {
    UUID commitmentId = insertFixtureCommitment();
    EmailToContactPenaltyConfig config =
        new EmailToContactPenaltyConfig("friend@example.com", "You missed your morning run.");
    PenaltyDispatchContext context = new PenaltyDispatchContext("Morning run", "Amin");

    int savedPort = javaMailSender.getPort();
    javaMailSender.setHost("127.0.0.1");
    javaMailSender.setPort(1);

    try {
      UUID eventId = penaltyEventLedger.insertPending(commitmentId, PenaltyType.EMAIL_TO_CONTACT);
      assertThat(penaltyEventLedger.claim(eventId)).isTrue();

      PenaltyDispatchResult result =
          penaltyDispatch
              .handlerFor(PenaltyType.EMAIL_TO_CONTACT)
              .dispatch(commitmentId, config, context);

      assertThat(result.success()).isFalse();
      assertThat(penaltyEventLedger.markFailed(eventId)).isTrue();
      assertThat(outcome(eventId)).isEqualTo("FAILED");
      assertThat(
              jdbcTemplate.queryForObject(
                  "SELECT COUNT(*) FROM penalty_events WHERE commitment_id = ?::uuid",
                  Integer.class,
                  commitmentId.toString()))
          .isEqualTo(1);
    } finally {
      javaMailSender.setHost("127.0.0.1");
      javaMailSender.setPort(savedPort);
    }
  }

  private String outcome(UUID eventId) {
    return jdbcTemplate.queryForObject(
        "SELECT outcome FROM penalty_events WHERE id = ?::uuid", String.class, eventId.toString());
  }

  private UUID insertFixtureCommitment() {
    UUID userId = UUID.randomUUID();
    UUID commitmentId = UUID.randomUUID();
    Instant now = Instant.now();

    jdbcTemplate.update(
        """
        INSERT INTO users (id, email, password_hash, first_name, last_name, created_at, updated_at)
        VALUES (?::uuid, ?, 'hash', 'Mail', 'Test', ?::timestamptz, ?::timestamptz)
        """,
        userId.toString(),
        "mail-" + userId + "@example.com",
        now.toString(),
        now.toString());

    jdbcTemplate.update(
        """
        INSERT INTO commitments (
            id, user_id, name, description, status, notify_time, deadline,
            mission_config, penalty_config, created_at, updated_at)
        VALUES (
            ?::uuid, ?::uuid, 'Mail fixture', NULL, 'PENDING',
            ?::timestamptz, ?::timestamptz,
            '{"_class":"QR_CODE","codePayload":"fixture"}'::jsonb,
            '{"_class":"EMAIL_TO_CONTACT","contactEmail":"a@b.co","message":"hi"}'::jsonb,
            ?::timestamptz, ?::timestamptz)
        """,
        commitmentId.toString(),
        userId.toString(),
        now.toString(),
        now.plusSeconds(3600).toString(),
        now.toString(),
        now.toString());

    return commitmentId;
  }
}

package com.waker.penalty.internal;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/** Single SMTP send path for EMAIL_TO_CONTACT (Mailpit locally, Postmark in prod). */
@Component
class SmtpEmailSender {

  private final JavaMailSender mailSender;
  private final MailProperties mailProperties;

  SmtpEmailSender(JavaMailSender mailSender, MailProperties mailProperties) {
    this.mailSender = mailSender;
    this.mailProperties = mailProperties;
  }

  void sendPlainText(String to, String subject, String body) throws MailException {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(mailProperties.from());
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(body, false);
      mailSender.send(message);
    } catch (MailException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new org.springframework.mail.MailSendException("Failed to build or send email", ex);
    }
  }
}

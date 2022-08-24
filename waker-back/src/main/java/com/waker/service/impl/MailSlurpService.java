package com.waker.service.impl;

import com.mailslurp.apis.InboxControllerApi;
import com.mailslurp.clients.ApiClient;
import com.mailslurp.clients.ApiException;
import com.mailslurp.clients.Configuration;
import com.mailslurp.clients.StringUtil;
import com.mailslurp.models.Email;
import com.mailslurp.models.ImapSmtpAccessDetails;
import com.mailslurp.models.InboxDto;
import com.mailslurp.models.InboxExistsDto;
import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MailSlurpService implements IMailServiceProvider {

    private MailSlurpService() {}
    private static MailSlurpService instance = null;
    public static MailSlurpService getInstance() {
        if (instance == null)
            instance = new MailSlurpService();
        return instance;
    }

    private static ApiClient apiClient;
    private static final Long TIMEOUT = 60000L;
    private static final String API_KEY = System.getenv("API_KEY");
    private static final String DEFAULT_INBOX_ID = System.getenv("DEFAULT_INBOX_ID");
    private static final String DEFAULT_INBOX_EMAIL = System.getenv("DEFAULT_INBOX_EMAIL");

    @Override
    public ResponseDTO<MailDTO> send(MailDTO mailDto) throws TechnicalException, BusinessException {
        if (StringUtils.isBlank(API_KEY)) {
            throw new TechnicalException(TechnicalErrorCodesAndMessages.INVALID_ENVIRONMENT_VARIABLE, "MailSlurp api key is not set");
        }
        // IMPORTANT set timeout for the http client
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();
        apiClient = Configuration.getDefaultApiClient();
        // IMPORTANT set api client timeouts
        apiClient.setConnectTimeout(TIMEOUT.intValue());
        apiClient.setWriteTimeout(TIMEOUT.intValue());
        apiClient.setReadTimeout(TIMEOUT.intValue());
        // IMPORTANT set API KEY and client
        apiClient.setHttpClient(httpClient);
        apiClient.setApiKey(API_KEY);

        try {
            InboxControllerApi inboxControllerApi = new InboxControllerApi(apiClient);

            if (StringUtils.isBlank(DEFAULT_INBOX_ID) || StringUtils.isBlank(DEFAULT_INBOX_EMAIL)) {
                throw new TechnicalException(TechnicalErrorCodesAndMessages.INVALID_ENVIRONMENT_VARIABLE, "MailSlurp default inbox id and email not set");
            }
            InboxExistsDto inboxExistsDto = inboxControllerApi.doesInboxExist(DEFAULT_INBOX_EMAIL);
            if (inboxExistsDto.getExists()) {
                throw new BusinessException(BusinessErrorCodesAndMessages.ERROR_404, "Inbox does not exist");
            }
            InboxDto inboxDto;
            if (mailDto.getMailFrom() != null && mailDto.getMailFrom().equals(DEFAULT_INBOX_EMAIL)) {
                inboxDto = inboxControllerApi.getInbox(UUID.fromString(DEFAULT_INBOX_ID));
            } else if (mailDto.getMailFrom() == null) {
                throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, "Sender email is undefined in mail dto");
            } else {
                inboxDto = inboxControllerApi.createInbox(mailDto.getMailFrom(), List.of(""), mailDto.getMailFromName(), "",
                        false, false, OffsetDateTime.of(LocalDateTime.now().plusDays(10), ZoneOffset.UTC),
                        LocalDateTime.now().plusDays(10).toEpochSecond(ZoneOffset.UTC), false, "customer", true);
            }
            ImapSmtpAccessDetails server = inboxControllerApi.getImapSmtpAccess(inboxDto.getId());
            Session mailSession = getMailSession(true, server);

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(mailDto.getMailFrom()));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(mailDto.getMailTo()));
            message.setSubject(mailDto.getSubject());

            String msg = mailDto.getText();
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);
            Transport.send(message);
        } catch (ApiException | MessagingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Session getMailSession(Boolean debug, ImapSmtpAccessDetails server) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.transport.protocol", "smtp");
        prop.put("mail.smtp.starttls.enable", debug.toString());
        prop.put("mail.debug", "false");
        prop.put("mail.smtp.host", server.getSmtpServerHost());
        prop.put("mail.smtp.port", server.getSmtpServerPort());


        class SMTPAuthenticator extends javax.mail.Authenticator {
            public PasswordAuthentication getPasswordAuthentication() {
                String username = server.getSmtpUsername();
                String password = server.getSmtpPassword();
                return new PasswordAuthentication(username, password);
            }
        }

        Authenticator auth = new SMTPAuthenticator();
        return Session.getInstance(prop, auth);
    }
}

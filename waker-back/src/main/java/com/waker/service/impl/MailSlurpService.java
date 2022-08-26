package com.waker.service.impl;

import com.mailslurp.apis.InboxControllerApi;
import com.mailslurp.clients.ApiClient;
import com.mailslurp.clients.ApiException;
import com.mailslurp.clients.Configuration;
import com.mailslurp.clients.StringUtil;
import com.mailslurp.models.*;
import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;

import javax.mail.*;
import javax.mail.internet.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MailSlurpService implements IMailServiceProvider {

    private MailSlurpService() {}
    private static MailSlurpService instance = null;
    public static MailSlurpService getInstance() {
        if (instance == null)
            instance = new MailSlurpService();
        return instance;
    }

    private static ApiClient apiClient = null;
    private static final Long TIMEOUT = 60000L;
    private static final String API_KEY = System.getenv("MAIL_SLURP_API_KEY");
    private static final String DEFAULT_INBOX_ID = System.getenv("MAIL_SLURP_DEFAULT_INBOX_ID");
    public static final String DEFAULT_INBOX_EMAIL = System.getenv("DEFAULT_INBOX_EMAIL");

    @Override
    public ResponseDTO<MailDTO> send(@NonNull MailDTO mailDto, @NonNull Boolean fromUs) {
        ResponseDTO<MailDTO> response;
        String sender = mailDto.getMailFrom();
        String recipient = mailDto.getMailTo();
        try {
            if (!fromUs && StringUtils.isBlank(sender)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, "Sender email is undefined in mail dto");
            }
            if (StringUtils.isBlank(recipient)) {
                throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, "Recipient email is undefined in mail dto");
            }

            InboxControllerApi inboxControllerApi = new InboxControllerApi(getApiClient());
            InboxDto inboxDto = null;
            String inboxType = CreateInboxDto.InboxTypeEnum.SMTP_INBOX.toString();

            OffsetDateTime expiresAt = OffsetDateTime.of(LocalDateTime.now().plusHours(10), ZoneOffset.UTC);
//            long expiresIn = LocalDateTime.now().plusHours(10).toEpochSecond(ZoneOffset.UTC);

            // email will be sent with client as the sender
            if (!fromUs) {
                inboxDto = inboxControllerApi.createInbox(sender, List.of(""), mailDto.getMailFromName(), "",
                        false, false, expiresAt, null, false, inboxType, false);
            }
            // email is from us and inbox was set up in dashboard and is of type SMTP
            if (fromUs && !StringUtils.isBlank(DEFAULT_INBOX_EMAIL) && !StringUtils.isBlank(DEFAULT_INBOX_ID) && inboxControllerApi.doesInboxExist(DEFAULT_INBOX_EMAIL).getExists()) {
                inboxDto = inboxControllerApi.getInbox(UUID.fromString(DEFAULT_INBOX_ID));
                inboxType = inboxDto.getInboxType().toString();
                sender = DEFAULT_INBOX_EMAIL;
            }
            // email is from us and inbox is either: not set up in dashboard, or is set up but not of type SMTP
            if (inboxDto == null || !inboxType.equals(CreateInboxDto.InboxTypeEnum.SMTP_INBOX.toString())) {
                CreateInboxDto opts = new CreateInboxDto();
                opts.setInboxType(CreateInboxDto.InboxTypeEnum.SMTP_INBOX);
                inboxDto = inboxControllerApi.createInboxWithOptions(opts);
                sender = inboxDto.getEmailAddress();
                inboxType = CreateInboxDto.InboxTypeEnum.SMTP_INBOX.toString();
            }
            ImapSmtpAccessDetails server = inboxControllerApi.getImapSmtpAccess(inboxDto.getId());
            Session mailSession = getMailSession(false, server);

            InboxDto toInbox = inboxControllerApi.createInbox(recipient, List.of(""), mailDto.getMailToName(), "Recipient inbox",
                    false, false, expiresAt, null, false, inboxType, false);

            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(sender));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toInbox.getEmailAddress()));
            message.setSubject(mailDto.getSubject());

            String msg = Optional.ofNullable(mailDto.getHtml()).orElse(mailDto.getText());
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);
            Transport.send(message);
            response = new ResponseDTO<>(mailDto, 200, "Email sent");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(mailDto, 500, "ERROR WHILE TRYING TO SEND EMAIL : " + e.getMessage());
        }
        return response;
    }

    @Override
    public String getMainEmail() {
        return DEFAULT_INBOX_EMAIL;
    }

    private ApiClient getApiClient() throws TechnicalException {
        if (apiClient == null) {
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
        }
        return apiClient;
    }

    private Session getMailSession(Boolean debug, ImapSmtpAccessDetails server) {
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.transport.protocol", "smtp");
        prop.put("mail.smtp.starttls.enable", "false");
        prop.put("mail.debug", debug.toString());
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

package com.waker.service.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;

import com.google.api.services.gmail.model.MessagePartBody;
import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

//import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Slf4j
public class GmailApiService implements IMailServiceProvider {

    private static GmailApiService instance = null;
    private static Credential credential = null;
    private GmailApiService() {
        /*try {
            credentials = getCredentials();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }*/
    }
    public static GmailApiService getInstance() {
        if (instance == null)
            instance = new GmailApiService();
        return instance;
    }

    private static final String EMAIL_ADDRESS = "amine.med.ma@gmail.com";
    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "waker-2";
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    /**
     * Directory to store authorization tokens for this application.
     */
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = List.of(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_METADATA);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private static final String SERVICE_ACCOUNT_KEY_FILE_PATH = "/service-account-key.json";
    private static final String SERVICE_ACCOUNT_KEY_P12FILE_PATH = "/service-account.p12";

    /**
     * Creates an authorized Credential object.
     *
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
            throws IOException, GeneralSecurityException {
        // Load client secrets.
        InputStream in = GmailApiService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(7777).build();
        // TODO: add some form of a permanent authorization of the internal gmail account for sending emails
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        //returns an authorized Credential object.
        return credential;
    }

    private static Credential getCredentialsFromServiceAccount(final NetHttpTransport HTTP_TRANSPORT) throws TechnicalException {
        try (InputStream in = GmailApiService.class.getResourceAsStream(SERVICE_ACCOUNT_KEY_P12FILE_PATH);) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + SERVICE_ACCOUNT_KEY_P12FILE_PATH);
            }
            /*
            * To use service accounts with gmail you need to have a Google Workspace domain account.
            * Then the admin of your domain will be able to set up domain wide delegation for the service account of the app.
            * It will only work with domain emails.
             * */
            return new  GoogleCredential.Builder()
                    .setTransport(HTTP_TRANSPORT)
                    .setJsonFactory(JSON_FACTORY)
                    .setServiceAccountId("amin-house@appspot.gserviceaccount.com")
                    .setServiceAccountScopes(SCOPES)
                    .setServiceAccountPrivateKeyFromP12File(in)
                    .setServiceAccountUser("aming@qwaker.co")
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            log.error(e.getMessage(), e);
            throw new TechnicalException(TechnicalErrorCodesAndMessages.UNDEFINED_EXCEPTION, "Man idkj  " + e.getMessage());
        }
    }

    private MimeMessage createEmail(String from, String to, String subject, String bodyText, String htmlBody) throws MessagingException, BusinessException {
        if (StringUtils.isBlank(from)) {
            throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, "Sender email is undefined");
        }
        if (StringUtils.isBlank(to)) {
            throw new BusinessException(BusinessErrorCodesAndMessages.MISSING_REQUIRED_FIELDS, "Recipient email is undefined");
        }
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
        message.setFrom(new InternetAddress(from));
        message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(bodyText);
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(htmlBody, "text/html; charset=utf-8");
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        message.setContent(multipart);
        return message;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);

        return new Message()
                .setRaw(Base64.encodeBase64URLSafeString(buffer.toByteArray()));
    }

    public ResponseDTO<?> init() {
        ResponseDTO<?> response;
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential;
            credential = getCredentialsFromServiceAccount(HTTP_TRANSPORT);
            Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
            String user = "me";
            /*ListLabelsResponse listResponse = service.users().labels().list(user).execute();
            List<Label> labels = listResponse.getLabels();*/

            var things = service.users().getProfile(user).execute();
            Collection<Object> values = things.values();
            if (values.isEmpty()) {
                System.out.println("Nothing found.");
                response = new ResponseDTO<>(null, 500, "No ''things'' found");
            } else {
                System.out.println("Things from your gmail:");
                for (Object value : values) {
                    System.out.printf("- %s\n", value);
                    log.info("{} \n", value);
                }
                response = new ResponseDTO<>(values, 200, "Whatever this is: ");

            }
        } catch (IOException | GeneralSecurityException | TechnicalException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(null, 500, "Error init gmail service: " + e.getMessage());
        }
        return response;
    }

    @Override
    public ResponseDTO<MailDTO> send(@NonNull MailDTO mailDto, @NonNull Boolean fromUs) {
        ResponseDTO<MailDTO> response;
        try {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);
            Gmail gmail = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            MimeMessage mimeMessage = createEmail(EMAIL_ADDRESS, mailDto.getMailTo(), mailDto.getSubject(), mailDto.getText(), mailDto.getHtml());
            Message message = createMessageWithEmail(mimeMessage);
            List<String> list = gmail.users()
                    .messages()
                    .send(EMAIL_ADDRESS, message)
                    .execute()
                    .getLabelIds();
            if (!list.contains("SENT")) {
                throw new TechnicalException(TechnicalErrorCodesAndMessages.UNDEFINED_EXCEPTION, "Email was not sent here is the list of labels: " + list);
            }
            response = new ResponseDTO<>(mailDto, 200, list.toString());
        } catch (GeneralSecurityException | IOException | MessagingException | BusinessException | TechnicalException e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(mailDto, 500, "Error sending email using gmail: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getMainEmail() {
        return null;
    }
}

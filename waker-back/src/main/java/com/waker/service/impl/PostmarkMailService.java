package com.waker.service.impl;

import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import com.wildbit.java.postmark.Postmark;
import com.wildbit.java.postmark.client.ApiClient;
import com.wildbit.java.postmark.client.data.model.message.Message;
import com.wildbit.java.postmark.client.data.model.message.MessageResponse;
import com.wildbit.java.postmark.client.exception.PostmarkException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

@Slf4j
public class PostmarkMailService implements IMailServiceProvider {

    private static final String EMAIL_ADDRESS = System.getenv("MAIN_EMAIL_ADDRESS");
    private static final String API_TOKEN = System.getenv("POSTMARK_API_TOKEN");

    // TODO: send email on behalf of end user
    @Override
    public ResponseDTO<MailDTO> send(@NonNull MailDTO mailDto, @NonNull Boolean fromUs) {
        ResponseDTO<MailDTO> responseDTO;

        try {
            if (StringUtils.isBlank(EMAIL_ADDRESS)) {
                throw new TechnicalException(TechnicalErrorCodesAndMessages.INVALID_ENVIRONMENT_VARIABLE, " Email address undefined.");
            }
            if (StringUtils.isBlank(API_TOKEN)) {
                throw new TechnicalException(TechnicalErrorCodesAndMessages.INVALID_ENVIRONMENT_VARIABLE, " Postmark api undefined.");
            }
            ApiClient client = Postmark.getApiClient(API_TOKEN);
            Message message = new Message(EMAIL_ADDRESS, mailDto.getMailTo(), mailDto.getSubject(), mailDto.getHtml());
            message.setMessageStream("outbound");
            MessageResponse response = client.deliverMessage(message);
            responseDTO = new ResponseDTO<>(mailDto, 200, "Email sent: " + response.getMessage());
        } catch (TechnicalException | PostmarkException | IOException e) {
            log.error(e.getMessage(), e);
            responseDTO = new ResponseDTO<>(mailDto, 500, "Error sending email using postmark: " + e.getMessage());
        }
        return responseDTO;
    }

    @Override
    public String getMainEmail() {
        return null;
    }

    @Override
    public ResponseDTO<?> test() {
        return null;
    }
}

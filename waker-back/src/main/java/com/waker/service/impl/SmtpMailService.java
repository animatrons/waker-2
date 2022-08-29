package com.waker.service.impl;

import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IMailServiceProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SmtpMailService implements IMailServiceProvider {

    private static final String GMAIL_ADDRESS = "";
    private static final String GMAIL_PASSWORD = "";
    private static final String GMAIL_HOST_NAME = "";

    private static final String OUTLOOK_EMAIL_ADDRESS = "";
    private static final String OUTLOOK_PASSWORD = "";
    private static final String OUTLOOK_HOST_NAME = "";

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
            


        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response = new ResponseDTO<>(mailDto, 500, "ERROR WHILE TRYING TO SEND EMAIL : " + e.getMessage());
        }
        return null;
    }

    @Override
    public String getMainEmail() {
        return null;
    }

    @Override
    public ResponseDTO<?> init() {
        return null;
    }

    private String resolveDomain(String email) throws BusinessException {
        String substring = StringUtils.substringBetween(email, "@", ".");
        return switch (substring) {
            case "gmail" -> GMAIL_HOST_NAME;
            case "outlook" -> OUTLOOK_HOST_NAME;
            default -> throw new BusinessException(BusinessErrorCodesAndMessages.ERROR_404, "Domain not yet supported");
        };
    }
}

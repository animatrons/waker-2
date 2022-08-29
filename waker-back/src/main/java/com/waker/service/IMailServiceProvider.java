package com.waker.service;

import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;
import lombok.NonNull;

public interface IMailServiceProvider {

    ResponseDTO<MailDTO> send(@NonNull MailDTO mailDto, @NonNull Boolean fromUs);
    String getMainEmail();

    ResponseDTO<?> init();
}

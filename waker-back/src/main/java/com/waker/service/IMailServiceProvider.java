package com.waker.service;

import com.waker.model.dto.MailDTO;
import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalException;

public interface IMailServiceProvider {

    ResponseDTO<MailDTO> send(MailDTO mailDto, Boolean fromUs) throws TechnicalException, BusinessException;
    String getMainEmail();
}

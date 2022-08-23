package com.waker.model.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BusinessException extends GeneralException {

    private int code;
    private String message;

    public BusinessException() {
        super();
    }

    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages) {
        super(codesAndMessages.toString());
        this.code = codesAndMessages.getCode();
        this.message = codesAndMessages.toString();
    }
    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages, String info) {
        super(codesAndMessages.toString() + "\n" + info);
        this.code = codesAndMessages.getCode();
        this.message = codesAndMessages.toString() + "\n" + info;
    }
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

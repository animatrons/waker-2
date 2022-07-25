package com.waker.model.exception;

public class BusinessException extends Exception {

    private String code;
    private String message;

    public BusinessException() {
        super();
    }
    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages) {
        super();
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString();
    }
    public BusinessException(String code, String message) {
        super();
        this.code = code;
        this.message = message;
    }
}

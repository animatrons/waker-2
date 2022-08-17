package com.waker.model.exception;

public class BusinessException extends Exception {

    private String code;
    private String message;

    public BusinessException() {
        super();
    }
    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages, String info) {
        super();
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString() + "\n" + info;
    }
    public BusinessException(String code, String message) {
        super();
        this.code = code;
        this.message = message;
    }
}

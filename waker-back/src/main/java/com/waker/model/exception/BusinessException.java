package com.waker.model.exception;

public class BusinessException extends Exception {

    private String code;
    private String message;

    public BusinessException() {
        super();
    }

    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages) {
        super(codesAndMessages.toString());
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString();
    }
    public BusinessException(BusinessErrorCodesAndMessages codesAndMessages, String info) {
        super(codesAndMessages.toString() + "\n" + info);
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString() + "\n" + info;
    }
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

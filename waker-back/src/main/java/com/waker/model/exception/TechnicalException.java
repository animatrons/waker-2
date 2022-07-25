package com.waker.model.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TechnicalException extends Exception {

    private String code;
    private String message;
    public TechnicalException() {
        super();
    }
    public TechnicalException(TechnicalErrorCodesAndMessages codesAndMessages) {
        super();
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString();
    }
    public TechnicalException(String code, String message) {
        super();
        this.code = code;
        this.message = message;
    }
}

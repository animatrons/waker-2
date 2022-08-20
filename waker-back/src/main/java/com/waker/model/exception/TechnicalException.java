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
        super(codesAndMessages.toString());
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString();
    }

    public TechnicalException(TechnicalErrorCodesAndMessages codesAndMessages, String info) {
        super(codesAndMessages.toString() + "  \n  " + info);
        this.code = codesAndMessages.name();
        this.message = codesAndMessages.toString() + "  \n  " + info;
    }
    public TechnicalException(String code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

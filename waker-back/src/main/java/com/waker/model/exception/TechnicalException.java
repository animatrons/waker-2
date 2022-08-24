package com.waker.model.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TechnicalException extends GeneralException {

    private int code;
    private String message;
    public TechnicalException() {
        super();
    }
    public TechnicalException(TechnicalErrorCodesAndMessages codesAndMessages) {
        super(codesAndMessages.toString());
        this.code = codesAndMessages.getCode();
        this.message = codesAndMessages.toString();
    }

    public TechnicalException(TechnicalErrorCodesAndMessages codesAndMessages, String info) {
        super(codesAndMessages.toString()  + "  [[" + info + "]]");
        this.code = codesAndMessages.getCode();
        this.message = codesAndMessages.toString() + "  [[" + info + "]]";
    }
    public TechnicalException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}

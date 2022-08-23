package com.waker.model.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeneralException extends Exception {

    private int code;
    private String message;
    public GeneralException() {
        super();
    }
    public GeneralException(String message) {
        super(message);
    }
}

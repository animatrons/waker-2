package com.waker.model.exception;

public enum TechnicalErrorCodesAndMessages {


    UNDEFINED_EXCEPTION(500) {
        public String toString() {
            return "UNDEFINED TECHNICAL EXCEPTION";
        }
    },
    DATABASE_ERROR(500) {
        public String toString() {
            return "DATABASE ERROR";
        }
    },
    DATABASE_ENV_VAR_UNDEFINED(500) {
        public String toString() {
            return "DATABASE ENVIRONMENT VARIABLES NOT DEFINED, PLEASE SET THEM";
        }
    };
    private final int code;
    TechnicalErrorCodesAndMessages(int code) {
        this.code = code;
    }
    public int getCode() {
        return this.code;
    }
}

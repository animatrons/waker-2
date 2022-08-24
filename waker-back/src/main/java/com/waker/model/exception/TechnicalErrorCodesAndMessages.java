package com.waker.model.exception;

public enum TechnicalErrorCodesAndMessages {


    UNDEFINED_EXCEPTION(500) {
        public String toString() {
            return "UNDEFINED TECHNICAL EXCEPTION";
        }
    },
    ENCRYPTION_ERROR(500) {
        public String toString() {
            return "ERROR ENCRYPTING OR DECRYPTING";
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
    },
    INVALID_ENVIRONMENT_VARIABLE(500) {
        public String toString() {
            return "ENVIRONMENT VARIABLES NOT DEFINED CORRECTLY";
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

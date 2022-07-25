package com.waker.model.exception;

public enum TechnicalErrorCodesAndMessages {

    UNDEFINED_EXCEPTION {
        public String toString() {
            return "UNDEFINED TECHNICAL EXCEPTION";
        }
    },
    DATABASE_ERROR {
        public String toString() {
            return "DATABASE ERROR";
        }
    },
    DATABASE_ENV_VAR_UNDEFINED {
        public String toString() {
            return "DATABASE ENVIRONMENT VARIABLES NOT DEFINED, PLEASE SET THEM";
        }
    }
}

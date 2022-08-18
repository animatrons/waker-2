package com.waker.model.exception;

public enum BusinessErrorCodesAndMessages {

    UNDEFINED_BUSINESS_EXCEPTION {
        public String toString() {
            return "UNDEFINED BUSINESS EXCEPTION";
        }
    },
    ERROR_404 {
        public String toString() {
            return "PAGE NOT FOUND";
        }
    },
    INVALID_VALUE_IN_FIELDS {
        public String toString() {
            return "INVALID VALUE IN FIELDS";
        }
    },
    MISSING_REQUIRED_FIELDS {
        public String toString() {
            return "MISSING REQUIRED FIELDS";
        }
    },
    LOGIN_ERROR {
        public String toString() {
            return "LOGIN ERROR";
        }
    },
}

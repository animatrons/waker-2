package com.waker.model.exception;

public enum BusinessErrorCodesAndMessages {

    UNDEFINED_BUSINESS_EXCEPTION(400) {
        public String toString() {
            return "UNDEFINED BUSINESS EXCEPTION";
        }
    },
    ERROR_404(404) {
        public String toString() {
            return "PAGE NOT FOUND";
        }
    },
    INVALID_VALUE_IN_FIELDS(400) {
        public String toString() {
            return "INVALID VALUE IN FIELDS";
        }
    },
    MISSING_REQUIRED_FIELDS(400) {
        public String toString() {
            return "MISSING REQUIRED FIELDS";
        }
    },
    LOGIN_ERROR(400) {
        public String toString() {
            return "LOGIN ERROR";
        }
    },

    UNAUTHORIZED(401) {
        public String toString() {
            return "UNAUTHORIZED";
        }
    },

    FORBIDDEN(403) {
        public String toString() {
            return "FORBIDDEN";
        }
    },
    ALREADY_EXISTS(406) {
        public String toString() {
            return "RESOURCE ALREADY EXISTS, CANNOT OVERRIDE";
        }
    };

    private final int code;
    BusinessErrorCodesAndMessages(int code) {
        this.code = code;
    }
    public int getCode() {
        return this.code;
    }
}

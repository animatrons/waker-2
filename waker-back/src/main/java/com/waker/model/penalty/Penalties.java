package com.waker.model.penalty;

public enum Penalties {

    GET_SCOLDED {
        public static final String name = "GetScolded";
        @Override
        public String toString() {
            return name;
        }
    },
    EMBARRASSING_TEXT {
        public static final String name = "EmbarrassingText";
        @Override
        public String toString() {
            return name;
        }
    },
    EMBARRASSING_EMAIL {
        public static final String name = "EmbarrassingEmail";
        @Override
        public String toString() {
            return name;
        }
    },
}

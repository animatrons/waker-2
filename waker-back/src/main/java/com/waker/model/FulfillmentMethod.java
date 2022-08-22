package com.waker.model;

import com.waker.model.penalty.Penalties;

import java.util.Arrays;
import java.util.Map;

public class FulfillmentMethod {

    private String name;
    private Map<Object, Object> setting;

    public boolean validate() {
        return name != null && Arrays.stream(Fulfillments.values()).anyMatch(f -> f.toString().equals(name));
    }
}

package com.waker.model.penalty;

import com.waker.model.penalty.config.APenaltyConfig;

public abstract class APenalty<T extends APenaltyConfig> {

    private T config;
}

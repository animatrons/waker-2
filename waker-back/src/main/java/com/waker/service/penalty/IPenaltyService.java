package com.waker.service.penalty;

import com.waker.model.penalty.config.APenaltyConfig;

public interface IPenaltyService<T extends APenaltyConfig> {

    void penalize(T config);
}

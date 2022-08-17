package com.waker.service.penalty;


import com.waker.model.penalty.APenalty;

public interface IPenaltyService<T extends APenalty> {

    void penalize(T penaltySettings);
}

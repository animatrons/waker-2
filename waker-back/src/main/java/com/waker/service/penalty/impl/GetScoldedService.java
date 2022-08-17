package com.waker.service.penalty.impl;

import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.impl.GetScolded;
import com.waker.service.penalty.IPenaltyService;

public class GetScoldedService implements IPenaltyService<GetScolded> {

    @Override
    public void penalize(GetScolded penaltySettings) {
        System.out.println("===========Roasting begins. Get ready============");
        System.out.println("...");
        System.out.printf("%s\n", penaltySettings.getScoldingMessage());
        System.out.println("!!!!!!!");
        System.out.println("...");
        System.out.println("===========You just got roasted============");
    }
}

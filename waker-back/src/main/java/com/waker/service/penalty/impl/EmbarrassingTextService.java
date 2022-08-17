package com.waker.service.penalty.impl;

import com.waker.model.penalty.impl.EmbarrassingText;
import com.waker.service.penalty.IPenaltyService;

public class EmbarrassingTextService implements IPenaltyService<EmbarrassingText> {

    @Override
    public void penalize(EmbarrassingText penaltySettings) {
        System.out.println("===========You shall pay for not making it on time============");
        System.out.println("This is you");
        System.out.println("...");
        System.out.printf("%s\n", penaltySettings.getText());
        System.out.println("!!!!!!!");
        System.out.println("...");
        System.out.println("===========You just got embarrassed============");
    }
}

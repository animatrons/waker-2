package com.waker.service.penalty.impl;

import com.waker.model.penalty.impl.EmbarrassingEmail;
import com.waker.service.EmailService;
import com.waker.service.penalty.IPenaltyService;

public class EmbarrassingEmailService implements IPenaltyService<EmbarrassingEmail> {

    @Override
    public void penalize(EmbarrassingEmail penaltySettings) {
        System.out.println("===========Sending embarrassing email begins============");
        EmailService.sendEmail(penaltySettings.getEmail());
        System.out.println("===========Ooof============");
    }
}

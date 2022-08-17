package com.waker.app;

import com.waker.model.exception.BusinessException;
import com.waker.model.penalty.APenalty;
import com.waker.service.penalty.IPenaltyService;
import com.waker.service.penalty.PenaltyFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PenaltyApp {

    private static PenaltyApp instance = null;
    private PenaltyApp() {};
    public static PenaltyApp getInstance() {
        return instance != null ? instance : (instance = new PenaltyApp());
    }

    // TODO: add ReminderApp with method to check reminder's status, update it and penalize when necessary

    private void punish(String method, APenalty penaltySettings) {
        PenaltyFactory penaltyFactory = PenaltyFactory.getInstance();
        System.out.printf("Penalty object signature %s\n", penaltySettings.getSignature());
        System.out.printf("Parameter penalty method name %s", method);
        try {
            IPenaltyService<APenalty> penaltyService = penaltyFactory.getService(method);
            penaltyService.penalize(penaltySettings);
        } catch (BusinessException e) {
            log.error(e.getMessage());
        }
    }
}

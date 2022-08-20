package com.waker.app;

import com.waker.model.dto.ResponseDTO;
import com.waker.model.exception.BusinessException;
import com.waker.model.penalty.APenalty;
import com.waker.service.penalty.IPenaltyService;
import com.waker.service.penalty.PenaltyFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PenaltyApp {

    private static PenaltyApp instance = null;
    private PenaltyApp() {}
    public static PenaltyApp getInstance() {
        return instance != null ? instance : (instance = new PenaltyApp());
    }

    // TODO: add ReminderApp with method to check reminder's status, update it and penalize when necessary

    private void punish(String method, APenalty penaltySettings) throws BusinessException {
        PenaltyFactory penaltyFactory = PenaltyFactory.getInstance();
        System.out.printf("Penalty object signature %s\n", penaltySettings.get_class());
        System.out.printf("Parameter penalty method name %s", method);
        IPenaltyService<APenalty> penaltyService = penaltyFactory.getService(method);
        penaltyService.penalize(penaltySettings);

    }

    public ResponseDTO<APenalty> takeAction(boolean toPunish, String method, APenalty penaltySettings) {
        ResponseDTO<APenalty> response;
        if (toPunish) {
            try {
                this.punish(method, penaltySettings);
                response = new ResponseDTO<>(penaltySettings, 200, "Punishment has been carried out with success");
            } catch (BusinessException e) {
                log.error(e.getMessage());
                response = new ResponseDTO<>(penaltySettings, 500, "Server Error while punishing: " + e.getMessage());
            }
            return response;
        }
        return new ResponseDTO<>(penaltySettings, 200, "Punishment was not carried out: ");
    }
}

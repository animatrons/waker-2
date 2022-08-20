package com.waker.service.penalty;

import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import com.waker.service.penalty.impl.EmbarrassingEmailService;
import com.waker.service.penalty.impl.EmbarrassingTextService;
import com.waker.service.penalty.impl.GetScoldedService;

import java.util.SplittableRandom;

public class PenaltyFactory {

    private static PenaltyFactory instance = null;
    private PenaltyFactory() {}
    public static PenaltyFactory getInstance() {
        return instance != null ? instance : (instance = new PenaltyFactory());
    }

    public <V extends IPenaltyService<? extends APenalty>> V getService(String signature) throws BusinessException {
        if (signature.equals(Penalties.EMBARRASSING_TEXT.toString())) {
            return (V) new EmbarrassingTextService();
        }
        if (signature.equals(Penalties.EMBARRASSING_EMAIL.toString())) {
            return (V) new EmbarrassingEmailService();
        }
        if (signature.equals(Penalties.GET_SCOLDED.toString())) {
            return (V) new GetScoldedService();
        }
        throw new BusinessException(BusinessErrorCodesAndMessages.ERROR_404, "Penalty signature given does not appear on the list.");
    }

}

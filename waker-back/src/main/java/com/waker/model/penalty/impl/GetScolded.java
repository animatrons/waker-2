package com.waker.model.penalty.impl;

import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class GetScolded extends APenalty {

    private String scoldingMessage;

    public GetScolded() {
        super(Penalties.GET_SCOLDED.toString());
    }
}

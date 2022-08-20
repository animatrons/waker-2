package com.waker.model.penalty.impl;

import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class EmbarrassingText extends APenalty {

    private String text;
    public EmbarrassingText() {
        super(Penalties.EMBARRASSING_TEXT.toString());
    }
}

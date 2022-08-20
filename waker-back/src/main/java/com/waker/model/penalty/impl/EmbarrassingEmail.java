package com.waker.model.penalty.impl;

import com.waker.model.Email;
import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class EmbarrassingEmail extends APenalty {

    private Email email;
    public EmbarrassingEmail() {
        super(Penalties.EMBARRASSING_EMAIL.toString());
    }
}

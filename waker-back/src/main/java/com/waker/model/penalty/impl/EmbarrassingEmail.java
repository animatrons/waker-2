package com.waker.model.penalty.impl;

import com.waker.model.Email;
import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EmbarrassingEmail extends APenalty {

    private final String signature = Penalties.EMBARRASSING_EMAIL.toString();
    private Email email;
}

package com.waker.model.penalty.impl;

import com.waker.model.penalty.APenalty;
import com.waker.model.penalty.Penalties;
import com.waker.model.penalty.config.GetScoldedConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GetScolded extends APenalty<GetScoldedConfig> {

    private final String name = Penalties.GET_SCOLDED.toString();

}

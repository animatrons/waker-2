package com.waker.model.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class ADto {

    private String key;

    public abstract boolean validate();
}

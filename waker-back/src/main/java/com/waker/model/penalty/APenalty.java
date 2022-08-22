package com.waker.model.penalty;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

@Getter
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_class")
public abstract class APenalty {

    /*
    * Must match one of these penalty class names, in Penalties enum
    * */
    private String _class;
    public APenalty(String _class) {
        this._class = _class;
    }

    public boolean validate() {
        return Arrays.stream(Penalties.values()).anyMatch(penalty -> penalty.toString().equals(this._class));
    }
}

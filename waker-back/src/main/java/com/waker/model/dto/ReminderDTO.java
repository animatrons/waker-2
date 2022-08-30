package com.waker.model.dto;

import com.waker.model.FulfillmentMethod;
import com.waker.model.User;
import com.waker.model.penalty.APenalty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class ReminderDTO extends ADto {

    private UserDTO user;
    private String name;
    private String description;
    private Date notifyTime;
    private Date deadline;
    private FulfillmentMethod fulfillmentMethod;
    private APenalty penaltySetting;
    private int status;
    private boolean active;

    @Override
    public boolean validate() {
        return user.validate("email") && name != null && !name.equals("") && notifyTime != null && deadline != null &&
            notifyTime.before(deadline) && penaltySetting != null && penaltySetting.validate() &&
                fulfillmentMethod != null && fulfillmentMethod.validate() &&
                    (status == -1 || status == 0 || status == 1);
    }

    @Override
    public boolean validate(String validationType) {
        return false;
    }
}

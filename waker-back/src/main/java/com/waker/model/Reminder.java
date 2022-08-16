package com.waker.model;

import com.waker.model.fulfillment.FulfillmentMethod;
import com.waker.model.penalty.APenalty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Reminder extends AModel {

    private final String collectionName = "reminder";

    private User user;
    private String name;
    private String description;
    private Date notifyTime;
    private Date deadline;
    private FulfillmentMethod fulfillmentMethod;
    private APenalty penaltySetting;
    /*
    * 1 : fulfilled
    * 0 : pending
    * -1 : not fulfilled
    * */
    private int status;

    private boolean active;

    @Override
    public String getCollectionName() {
        return collectionName;
    }
}

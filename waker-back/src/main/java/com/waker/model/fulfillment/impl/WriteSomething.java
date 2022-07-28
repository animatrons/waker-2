package com.waker.model.fulfillment.impl;

import com.waker.model.fulfillment.AFulfillment;
import com.waker.model.fulfillment.Fulfillments;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WriteSomething extends AFulfillment<String> {

    public String name = Fulfillments.WRITE_SOMETHING.toString();
}

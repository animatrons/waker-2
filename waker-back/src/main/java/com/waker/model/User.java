package com.waker.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User extends AModel {

    private final String collectionName = "users";

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String country;
    private Address address;
    private Date birthDay;
    private String phone;
//    private String stripeCustomerId;
    private String token;

    @Override
    public String getCollectionName() {
        return collectionName;
    }
}

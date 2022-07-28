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

    private final String collectionName = "user";

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String country;
    private Date birthDay;
    private String phone;

    @Override
    public String getCollectionName() {
        return collectionName;
    }
}

package com.waker.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class UserDTO extends ADto {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String country;
    private AddressDTO address;
    private Date birthDay;
    private String phone;
    //    private String stripeCustomerId;
    private String token;

    @Override
    public boolean validate() {
        return firstName != null && !firstName.equals("") && lastName != null && !lastName.equals("") && email != null && !email.equals("")
                && password != null && !password.equals("");
    }

    public boolean validateOnLogin() {
        return email != null && !email.equals("") && password != null && !password.equals("");
    }
}

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

    /**
     * Validates based on validation type. Only the following types are accepted:
     *      login, email, id, all
     * @param type type of validation, can be one of these: login, email, id, all
     * @return boolean result
     */
    @Override
    public boolean validate(String type) {
        switch (type){
            case "login" -> {
                return email != null && !email.equals("") && password != null && !password.equals("");
            }
            case "email" -> {
                return email != null && !email.equals("");
            }
            case "id" -> {
                return getKey() != null && !getKey().equals("");
            }
            case "all" -> {
                return firstName != null && !firstName.equals("") && lastName != null && !lastName.equals("") && email != null && !email.equals("")
                        && password != null && !password.equals("");
            }
            default -> {
                return false;
            }
        }
    }

}

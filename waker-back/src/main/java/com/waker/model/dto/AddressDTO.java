package com.waker.model.dto;

public class AddressDTO extends ADto {

    private String city;
    private String address1;
    private String address2;
    private String address3;
    private String zipCode;
    private String number;

    @Override
    public boolean validate() {
        return city != null && !city.equals("") && address1 != null && !address1.equals("");
    }

    @Override
    public boolean validate(String validationType) {
        return false;
    }
}

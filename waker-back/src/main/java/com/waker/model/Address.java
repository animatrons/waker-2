package com.waker.model;

public class Address extends AModel {

    private final String collectionName = null;
    private String city;
    private String address1;
    private String address2;
    private String address3;
    private String zipCode;
    private String number;
    @Override
    public String getCollectionName() {
        return collectionName;
    }
}

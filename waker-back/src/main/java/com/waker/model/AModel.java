package com.waker.model;

import lombok.Getter;
import lombok.Setter;
import org.jongo.marshall.jackson.oid.MongoId;
import org.jongo.marshall.jackson.oid.MongoObjectId;

import java.util.Date;

@Getter
@Setter
public abstract class AModel {

    @MongoId
    @MongoObjectId
    private String key;
    private Date createdAt;
    private Date updatedAt;
    private String code;
    private String label;
    private boolean enabled = false;

    public AModel() {
        this.createdAt = new Date(System.currentTimeMillis());
    }
    public abstract String getCollectionName();

}

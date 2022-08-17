package com.waker.model;

import lombok.Getter;

import java.util.Date;

@Getter
public class Email {

    private String from;
    private String to;
    private String subject;
    private String body;
    private Date date;
}

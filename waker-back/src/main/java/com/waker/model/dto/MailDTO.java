package com.waker.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MailDTO {

    private String mailFrom;
    private String mailFromName;
    private String mailTo;
    private String mailToName;
    private String subject;
    private String text;
    private String html;
}

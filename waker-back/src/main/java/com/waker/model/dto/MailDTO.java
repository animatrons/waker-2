package com.waker.model.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MailDTO {

    private String mailFrom;
    private String mailFromName;
    private String mailTo;
    private String mailToName;
    private String subject;
    private String text;
    private String html;

    /*public MailDTO(String mailFrom, String mailFromName,
                   String mailTo, String mailToName, String subject, String text, String html) {

    }*/
}

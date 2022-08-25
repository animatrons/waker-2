package com.waker.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Getter
@Setter
public class UserOutputDTO {

    private String firstName;
    private String lastName;
    private String subject;
    private String accessToken;
    private String refreshToken;
    private final long issuedAt;

    public UserOutputDTO() {
        issuedAt = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
    }
}

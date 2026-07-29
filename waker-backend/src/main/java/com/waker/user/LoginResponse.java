package com.waker.user;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {}

package com.waker.user;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
    UUID id, String email, String firstName, String lastName, Instant createdAt) {}

package com.waker.user;

import java.util.UUID;

public interface UserService {

  UserResponse register(RegisterUserRequest request);

  LoginResponse login(LoginRequest request);

  /** Pessimistic lock on the user row — serializes commitment creates per user (Story 2.3). */
  void lockById(UUID userId);

  /** Read-only lookup for commitment dispatcher display-name handoff (Story 3.5). */
  UserResponse getById(UUID id);
}

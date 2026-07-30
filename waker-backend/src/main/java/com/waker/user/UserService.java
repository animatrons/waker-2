package com.waker.user;

public interface UserService {

  UserResponse register(RegisterUserRequest request);

  LoginResponse login(LoginRequest request);

  /** Pessimistic lock on the user row — serializes commitment creates per user (Story 2.3). */
  void lockById(java.util.UUID userId);
}

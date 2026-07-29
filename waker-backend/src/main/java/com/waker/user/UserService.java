package com.waker.user;

public interface UserService {

  UserResponse register(RegisterUserRequest request);

  LoginResponse login(LoginRequest request);
}

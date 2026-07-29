package com.waker.common;

/** Thrown when login fails for unknown email or wrong password (identical message). */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException() {
    super("Invalid email or password");
  }
}

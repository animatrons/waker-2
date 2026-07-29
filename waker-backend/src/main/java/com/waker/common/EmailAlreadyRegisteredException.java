package com.waker.common;

/** Thrown when registration hits the users.email unique constraint. */
public class EmailAlreadyRegisteredException extends RuntimeException {

  public EmailAlreadyRegisteredException() {
    super("Email already registered");
  }
}

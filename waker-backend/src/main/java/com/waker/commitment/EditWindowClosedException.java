package com.waker.commitment;

public class EditWindowClosedException extends RuntimeException {

  public EditWindowClosedException() {
    super("Edit window is closed");
  }
}

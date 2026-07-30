package com.waker.commitment;

public class InvalidCommitmentStateException extends RuntimeException {

  public InvalidCommitmentStateException() {
    super("Commitment cannot be modified in its current state");
  }
}

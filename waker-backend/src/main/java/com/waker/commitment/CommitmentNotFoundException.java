package com.waker.commitment;

public class CommitmentNotFoundException extends RuntimeException {

  public CommitmentNotFoundException() {
    super("Commitment not found");
  }
}

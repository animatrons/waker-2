package com.waker.commitment;

public class ConcurrentCommitmentCapExceededException extends RuntimeException {

  public ConcurrentCommitmentCapExceededException() {
    super("Maximum concurrent pending commitments exceeded");
  }
}

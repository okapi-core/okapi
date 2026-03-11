package org.okapi.exceptions;

public class DataFailureException extends RuntimeException {
  public DataFailureException() {
  }

  public DataFailureException(String message) {
    super(message);
  }
}

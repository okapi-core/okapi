package org.okapi.data.exceptions;

public class TooManyRetriesException extends Exception {
  public TooManyRetriesException(String message) {
    super(message);
  }
}

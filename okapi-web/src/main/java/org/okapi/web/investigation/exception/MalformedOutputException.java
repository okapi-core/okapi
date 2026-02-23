package org.okapi.web.investigation.exception;

import lombok.experimental.StandardException;

@StandardException
public class MalformedOutputException extends RuntimeException {
  public MalformedOutputException(String message) {
    super(message);
  }
}

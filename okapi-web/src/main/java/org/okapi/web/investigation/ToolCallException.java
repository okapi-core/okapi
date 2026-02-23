package org.okapi.web.investigation;

import lombok.experimental.StandardException;

@StandardException
public class ToolCallException extends Exception{
  public ToolCallException(String message) {
    super(message);
  }
}

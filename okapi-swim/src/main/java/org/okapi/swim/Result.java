package org.okapi.swim;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Result<R, E extends Exception> {
  R result;
  E exception;
}

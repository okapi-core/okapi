package org.okapi.swim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class Result<R, E extends Exception> {
  R result;
  E exception;
}

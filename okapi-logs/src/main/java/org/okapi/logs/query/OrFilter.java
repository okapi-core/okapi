package org.okapi.logs.query;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class OrFilter extends LogFilter {
  LogFilter left;
  LogFilter right;

  @Override
  public Kind kind() {
    return Kind.OR;
  }
}

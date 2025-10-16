package org.okapi.logs.query;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class RegexFilter extends LogFilter {
  String regex;

  @Override
  public Kind kind() {
    return Kind.REGEX;
  }
}

package org.okapi.logs.query;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public class LevelFilter extends LogFilter {
  int levelCode;

  public int levelCode() {
    return levelCode;
  }

  @Override
  public Kind kind() {
    return Kind.LEVEL;
  }
}

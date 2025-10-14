package org.okapi.traces.query;

import com.google.re2j.Pattern;
import lombok.Getter;

@Getter
public class AttributeFilter {
  private final String name;
  private final String value; // nullable
  private final Pattern pattern; // nullable

  public AttributeFilter(String name, String value) {
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
    if (value == null) throw new IllegalArgumentException("value required");
    this.name = name;
    this.value = value;
    this.pattern = null;
  }

  public static AttributeFilter withPattern(String name, String pattern) {
    if (name == null || name.isEmpty()) throw new IllegalArgumentException("name required");
    if (pattern == null) throw new IllegalArgumentException("pattern required");
    AttributeFilter f = new AttributeFilter(name, Pattern.compile(pattern));
    return f;
  }

  private AttributeFilter(String name, Pattern pattern) {
    this.name = name;
    this.value = null;
    this.pattern = pattern;
  }

  public boolean isPattern() {
    return pattern != null;
  }
}

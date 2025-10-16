package org.okapi.logs.query;

public abstract class LogFilter {
  public enum Kind { REGEX, TRACE, LEVEL, AND, OR }
  public abstract Kind kind();
}


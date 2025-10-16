package org.okapi.logs.query;

import lombok.Value;

@Value
public class TraceFilter extends LogFilter {
  String traceId;
  @Override
  public Kind kind() { return Kind.TRACE; }
}


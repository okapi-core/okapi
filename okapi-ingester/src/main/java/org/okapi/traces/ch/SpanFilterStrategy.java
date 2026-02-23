package org.okapi.traces.ch;

import io.opentelemetry.proto.trace.v1.Span;

public interface SpanFilterStrategy {
  boolean shouldPrune(Span span);
}

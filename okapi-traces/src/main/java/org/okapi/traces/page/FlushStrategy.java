package org.okapi.traces.page;

public interface FlushStrategy {
  boolean shouldFlush(SpanPage page);
}

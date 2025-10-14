package org.okapi.traces.query;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.util.List;

public interface TraceQueryProcessor extends AutoCloseable {
  List<Span> getSpans(long start, long end, String tenantId, String application, String traceId)
      throws IOException;

  List<Span> getSpans(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException;

  List<Span> getTrace(long start, long end, String tenantId, String application, String spanId)
      throws IOException;

  @Override
  default void close() {}
}


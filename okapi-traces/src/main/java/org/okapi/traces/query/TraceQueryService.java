package org.okapi.traces.query;

import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TraceQueryService implements AutoCloseable {

  private final TraceQueryProcessor processor;

  public TraceQueryService(Path baseDir) {
    this.processor = new TraceFileQueryProcessor(baseDir);
  }

  public TraceQueryService(TraceQueryProcessor processor) {
    this.processor = processor;
  }

  public List<Span> getSpans(
      long start, long end, String tenantId, String application, String traceId)
      throws IOException {
    return processor.getSpansWithFilter(start, end, tenantId, application, traceId);
  }

  public List<Span> getSpans(
      long start, long end, String tenantId, String application, AttributeFilter filter)
      throws IOException {
    return processor.getSpansWithFilter(start, end, tenantId, application, filter);
  }

  public List<Span> getTrace(
      long start, long end, String tenantId, String application, String spanId) throws IOException {
    return processor.getTrace(start, end, tenantId, application, spanId);
  }

  @Override
  public void close() {
    try {
      processor.close();
    } catch (Exception ignored) {
    }
  }
}

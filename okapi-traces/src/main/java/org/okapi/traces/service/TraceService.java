package org.okapi.traces.service;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.RequiredArgsConstructor;
import org.okapi.traces.page.TraceBufferPoolManager;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class TraceService {

  private final TraceBufferPoolManager traceBufferPoolManager;

  // Ingest raw OTLP payloads into BufferPoolManager pages per tenant/app
  public int ingestOtelProtobuf(byte[] body, String tenant, String application) {
    try {
      var req = ExportTraceServiceRequest.parseFrom(body);
      traceBufferPoolManager.consume(tenant, application, req);
      // Count spans directly using current OTLP types
      int count = 0;
      for (var rs : req.getResourceSpansList()) {
        for (var ss : rs.getScopeSpansList()) {
          count += ss.getSpansCount();
        }
      }
      return count;
    } catch (Exception e) {
      return 0;
    }
  }
}

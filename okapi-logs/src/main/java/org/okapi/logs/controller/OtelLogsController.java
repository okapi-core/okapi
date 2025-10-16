package org.okapi.logs.controller;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.mappers.OtelToLogMapper;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OtelLogsController {
  private final LogPageBufferPool bufferPool;
  private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

  @PostMapping(path = "/v1/logs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public String ingestProtobuf(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody byte[] body)
      throws IOException {
    ExportLogsServiceRequest req = ExportLogsServiceRequest.parseFrom(body);
    AtomicLong count = new AtomicLong();
    for (ResourceLogs rl : req.getResourceLogsList()) {
      for (ScopeLogs sl : rl.getScopeLogsList()) {
        for (LogRecord lr : sl.getLogRecordsList()) {
          long tsMs = lr.getTimeUnixNano() / 1_000_000L;
          int level = OtelToLogMapper.mapLevel(lr.getSeverityNumber().getNumber());
          String traceId = OtelToLogMapper.traceIdToHex(lr.getTraceId());
          String text = OtelToLogMapper.anyValueToString(lr.getBody());
          bufferPool.consume(tenantId, logStream, tsMs, traceId, level, text);
          count.incrementAndGet();
        }
      }
    }
    meterRegistry.counter("okapi.logs.ingest_records_total").increment(count.get());
    return "ingested=" + count.get();
  }
}

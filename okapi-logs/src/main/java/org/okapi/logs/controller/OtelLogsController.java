package org.okapi.logs.controller;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.okapi.logs.mappers.OtelToLogMapper;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<String> ingestProtobuf(
      @RequestHeader("X-Okapi-Tenant-Id") String tenantId,
      @RequestHeader("X-Okapi-Log-Stream") String logStream,
      @RequestBody byte[] body)
      throws IOException {
    var req = ExportLogsServiceRequest.parseFrom(body);
    var count = 0;
    for (var resourceLog : req.getResourceLogsList()) {
      for (var scopeLogs : resourceLog.getScopeLogsList()) {
        for (LogRecord logRecord : scopeLogs.getLogRecordsList()) {
          long tsMs = logRecord.getTimeUnixNano() / 1_000_000L;
          int level = OtelToLogMapper.mapLevel(logRecord.getSeverityNumber().getNumber());
          String traceId = OtelToLogMapper.traceIdToHex(logRecord.getTraceId());
          String text = OtelToLogMapper.anyValueToString(logRecord.getBody());
          bufferPool.consume(tenantId, logStream, tsMs, traceId, level, text);
          count++;
        }
      }
    }
    meterRegistry.counter("okapi.logs.ingest_records_total").increment(count);
    return ResponseEntity.ok("OK");
  }
}

package org.okapi.traces.api;

import java.util.Map;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/traces")
public class IngestionController {

  @Autowired private TraceService traceService;

  // OTLP/HTTP compatible ingest endpoint (JSON)
  @PostMapping(value = "", consumes = "application/json")
  public ResponseEntity<Map<String, Object>> ingest(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestHeader("X-Okapi-App") String application,
      @RequestBody String otlpTraces) {
    validateTenant(tenant);
    validateApp(application);
    // Parse JSON into OTLP ExportTraceServiceRequest then use buffer pool path
    int count;
    try {
      var builder =
          io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest.newBuilder();
      com.google.protobuf.util.JsonFormat.parser()
          .ignoringUnknownFields()
          .merge(otlpTraces, builder);
      var req = builder.build();
      // serialize back to bytes to reuse service method
      count = traceService.ingestOtelProtobuf(req.toByteArray(), tenant, application);
    } catch (Exception e) {
      count = 0;
    }
    return ResponseEntity.ok(Map.of("ingested", count));
  }

  // OTLP/HTTP compatible ingest endpoint (Protobuf)
  @PostMapping(value = "", consumes = "application/x-protobuf")
  public ResponseEntity<Map<String, Object>> ingestProtobuf(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestHeader("X-Okapi-App") String application,
      @RequestBody byte[] body) {
    validateTenant(tenant);
    validateApp(application);
    int count = traceService.ingestOtelProtobuf(body, tenant, application);
    return ResponseEntity.ok(Map.of("ingested", count));
  }

  private static void validateTenant(String tenant) {
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalArgumentException("Missing X-Okapi-Tenant-Id header");
    }
  }

  private static void validateApp(String application) {
    if (application == null || application.isBlank()) {
      throw new IllegalArgumentException("Missing X-Okapi-App header");
    }
  }
}

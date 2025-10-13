package org.okapi.traces.api;

import java.util.List;
import java.util.Map;

import org.okapi.traces.model.OkapiSpan;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/traces")
public class TraceController {

  @Autowired private TraceService traceService;

  // OTLP/HTTP compatible ingest endpoint (JSON)
  @PostMapping(value = "", consumes = "application/json")
  public ResponseEntity<Map<String, Object>> ingest(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestBody String otlpTraces) {
    validateTenant(tenant);
    int count = traceService.ingestOtelJson(otlpTraces, tenant);
    return ResponseEntity.ok(Map.of("ingested", count));
  }

  // OTLP/HTTP compatible ingest endpoint (Protobuf)
  @PostMapping(value = "", consumes = "application/x-protobuf")
  public ResponseEntity<Map<String, Object>> ingestProtobuf(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestBody byte[] body) {
    validateTenant(tenant);
    int count = traceService.ingestOtelProtobuf(body, tenant);
    return ResponseEntity.ok(Map.of("ingested", count));
  }

  // List all spans for a trace
  @GetMapping("/{traceId}/spans")
  public List<OkapiSpan> getSpans(
      @PathVariable String traceId,
      @RequestHeader("X-Okapi-Tenant-Id") String tenant) {
    validateTenant(tenant);
    return traceService.getSpans(traceId, tenant);
  }

  // Get span metadata by span-id
  @GetMapping("/span/{spanId}")
  public ResponseEntity<OkapiSpan> getSpan(
      @PathVariable String spanId,
      @RequestHeader("X-Okapi-Tenant-Id") String tenant) {
    validateTenant(tenant);
    return traceService
        .getSpanById(spanId, tenant)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // List spans by duration for a given application and time window
  @GetMapping("/spans/by-duration")
  public List<OkapiSpan> listByDuration(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestParam("startMillis") long startMillis,
      @RequestParam("endMillis") long endMillis,
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
    validateTenant(tenant);
    return traceService.listByDuration(tenant, startMillis, endMillis, limit);
  }

  // List traces within time window and how many have error spans
  @GetMapping("/traces/by-window")
  public Map<String, Object> listTracesByWindow(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestParam("startMillis") long startMillis,
      @RequestParam("endMillis") long endMillis) {
    validateTenant(tenant);
    return traceService.listTracesByWindow(tenant, startMillis, endMillis);
  }

  // List spans of type error in a window
  @GetMapping("/spans/errors")
  public List<OkapiSpan> listErrorSpans(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestParam("startMillis") long startMillis,
      @RequestParam("endMillis") long endMillis,
      @RequestParam(value = "limit", required = false, defaultValue = "1000") int limit) {
    validateTenant(tenant);
    return traceService.listErrorSpans(tenant, startMillis, endMillis, limit);
  }

  // Histogram of spans of type error and ok, minutely granularity
  @GetMapping("/spans/histogram")
  public Map<Long, Map<String, Long>> spanHistogram(
      @RequestHeader("X-Okapi-Tenant-Id") String tenant,
      @RequestParam("startMillis") long startMillis,
      @RequestParam("endMillis") long endMillis) {
    validateTenant(tenant);
    return traceService.spanHistogramByMinute(tenant, startMillis, endMillis);
  }

  private static void validateTenant(String tenant) {
    if (tenant == null || tenant.isBlank()) {
      throw new IllegalArgumentException("Missing X-Okapi-Tenant-Id header");
    }
  }
}

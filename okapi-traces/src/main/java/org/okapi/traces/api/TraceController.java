package org.okapi.traces.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.okapi.traces.model.Span;
import org.okapi.traces.service.TraceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/traces")
public class TraceController {

  @Autowired private TraceService traceService;

  // OTLP/HTTP compatible ingest endpoint (JSON)
  @PostMapping("")
  public ResponseEntity<Map<String, Object>> ingest(
      @RequestHeader("X-Okapi-Tenant") String tenant,
      @RequestHeader("X-Okapi-App") String app,
      @RequestBody JsonNode otlpTraces) {
    validateTenantAndApp(tenant, app);
    int count = traceService.ingestOtelJson(otlpTraces, tenant, app);
    return ResponseEntity.ok(Map.of("ingested", count));
  }

  // List all spans for a trace
  @GetMapping("/{traceId}/spans")
  public List<Span> getSpans(
      @PathVariable String traceId,
      @RequestHeader("X-Okapi-Tenant") String tenant,
      @RequestHeader("X-Okapi-App") String app) {
    validateTenantAndApp(tenant, app);
    return traceService.getSpans(traceId, tenant, app);
  }

  // Get span metadata by span-id
  @GetMapping("/span/{spanId}")
  public ResponseEntity<Span> getSpan(
      @PathVariable String spanId,
      @RequestHeader("X-Okapi-Tenant") String tenant,
      @RequestHeader("X-Okapi-App") String app) {
    validateTenantAndApp(tenant, app);
    return traceService
        .getSpanById(spanId, tenant, app)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  // List spans by duration for a given application and time window
  @GetMapping("/spans/by-duration")
  public List<Span> listByDuration(
      @RequestHeader("X-Okapi-Tenant") String tenant,
      @RequestHeader("X-Okapi-App") String app,
      @RequestParam("startMillis") long startMillis,
      @RequestParam("endMillis") long endMillis,
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
    validateTenantAndApp(tenant, app);
    return traceService.listByDuration(tenant, app, startMillis, endMillis, limit);
  }

  private static void validateTenantAndApp(String tenant, String app) {
    if (tenant == null || tenant.isBlank() || app == null || app.isBlank()) {
      throw new IllegalArgumentException("Missing X-Okapi-Tenant or X-Okapi-App header");
    }
  }
}

package org.okapi.metrics.spring.controller;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.otel.OtelConverter;
import org.okapi.metrics.service.runnables.MetricsWriter;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/metrics")
public class OtelController {

  @Autowired private MetricsWriter metricsWriter;

  private final OtelConverter converter = new OtelConverter();

  @PostMapping(value = "", consumes = "application/x-protobuf", produces = "application/x-protobuf")
  public ResponseEntity<ExportMetricsServiceResponse> export(
      @RequestHeader("X-Okapi-Tenant") String tenant,
      @RequestBody ExportMetricsServiceRequest request) {
    try {
      if (tenant == null || tenant.isBlank()) {
        log.debug("Tenant not specified.");
        return ResponseEntity.badRequest().body(ExportMetricsServiceResponse.getDefaultInstance());
      }

      List<ExportMetricsRequest> converted = converter.toOkapiRequests(tenant, request);
      for (ExportMetricsRequest r : converted) {
        log.debug("Processing {} {} {}", r.getTenantId(), r.getMetricName(), r.getTags());
        metricsWriter.onRequestArrive(r);
      }
      return ResponseEntity.ok(ExportMetricsServiceResponse.getDefaultInstance());
    } catch (Exception e) {
      log.warn("Failed to ingest OTLP metrics: {}", e.getMessage());
      return ResponseEntity.ok(ExportMetricsServiceResponse.getDefaultInstance());
    }
  }
}

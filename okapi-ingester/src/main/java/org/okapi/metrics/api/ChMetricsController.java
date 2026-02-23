/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.api;

import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.ch.ChMetricsIngester;
import org.okapi.metrics.otel.ConversionConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ChMetricsController {
  @Autowired ChMetricsIngester metricsIngester;

  @PostMapping(
      path = "/v1/metrics",
      consumes = {MediaType.APPLICATION_PROTOBUF_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE})
  public ResponseEntity<Void> ingest(
      @RequestBody byte[] body,
      @RequestHeader(value = "x-okapi-metrics-dialect", required = false) String dialect)
      throws Exception {
    var otlpMetrics = ExportMetricsServiceRequest.parseFrom(body);
    metricsIngester.ingestOtelProtobuf(otlpMetrics, ConversionConfig.fromHeader(dialect));
    return ResponseEntity.ok().build();
  }
}

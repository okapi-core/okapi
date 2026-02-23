/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.api;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import lombok.extern.slf4j.Slf4j;
import org.okapi.traces.ch.ChTracesIngester;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class OtelTracesController {

  @Autowired private ChTracesIngester tracesIngester;

  @PostMapping(path = "/v1/traces", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Void> ingest(@RequestBody byte[] body) throws Exception {
    var otlpTraces = ExportTraceServiceRequest.parseFrom(body);
    tracesIngester.ingest(otlpTraces);
    return ResponseEntity.ok().build();
  }
}

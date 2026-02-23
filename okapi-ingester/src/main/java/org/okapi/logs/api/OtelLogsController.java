/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.api;

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.okapi.logs.ch.ChLogsIngester;
import org.okapi.wal.io.IllegalWalEntryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class OtelLogsController {

  @Autowired ChLogsIngester logsIngester;

  @PostMapping(path = "/v1/logs", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public ResponseEntity<Void> ingestProtobuf(@RequestBody byte[] body)
      throws IOException, IllegalWalEntryException {
    var req = ExportLogsServiceRequest.parseFrom(body);
    logsIngester.ingest(req);
    return ResponseEntity.ok().build();
  }
}

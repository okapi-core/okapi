/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.ch;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.io.IOException;
import java.util.List;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.IllegalWalEntryException;

public class ChTracesIngester {
  private final ChWalResources walResources;

  public ChTracesIngester(ChWalResources walResources) {
    this.walResources = walResources;
  }

  public void ingest(ExportTraceServiceRequest request)
      throws BadRequestException, IllegalWalEntryException, IOException {
    ChTracesValidator.validate(request);
    walResources.getWriter().appendBatch(List.of(toWalEntry(request)));
  }

  private WalEntry toWalEntry(ExportTraceServiceRequest request) throws IOException {
    var lsnSupplier = this.walResources.getSupplier();
    return new WalEntry(lsnSupplier.next(), request.toByteArray());
  }
}

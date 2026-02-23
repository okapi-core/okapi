/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import static org.okapi.metrics.service.MetricsValidator.validate;

import com.google.gson.Gson;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.otel.OtelConverter;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.ForwardedMetricsRequest;
import org.okapi.wal.frame.WalEntry;
import org.okapi.wal.io.IllegalWalEntryException;

@RequiredArgsConstructor
public class MetricsIngester {

  private final OtelConverter otelConverter;
  private final WalResourcesPerStream<Integer> walResourcesPerStream;
  private final MetricsGrouper metricsGrouper;
  private final Gson gson = new Gson();

  public void ingestOtelProtobuf(ExportMetricsServiceRequest exportMetricsServiceRequest)
      throws BadRequestException, IllegalWalEntryException, IOException {
    List<ExportMetricsRequest> converted =
        otelConverter.toOkapiRequests(exportMetricsServiceRequest);
    validate(converted);
    var groupByShard = metricsGrouper.groupByShard(converted);
    for (var shard : groupByShard.keySet()) {
      var batch = groupByShard.get(shard);
      var walEntries = batch.stream().map((r) -> toWalEntry(shard, r)).toList();
      walResourcesPerStream.getWalWriter(shard).appendBatch(walEntries);
    }
  }

  protected WalEntry toWalEntry(int shard, ExportMetricsRequest request) {
    var lsnSupplier = walResourcesPerStream.getLsnSupplier(shard);
    var payload = gson.toJson(request);
    return new WalEntry(lsnSupplier.next(), payload.getBytes());
  }

  public void ingestForwarded(ForwardedMetricsRequest forwardedMetricsRequest)
      throws IllegalWalEntryException, IOException {
    var shard = forwardedMetricsRequest.getShardId();
    var walEntries =
        forwardedMetricsRequest.getMetricsRequests().stream()
            .map(r -> toWalEntry(shard, r))
            .toList();
    walResourcesPerStream.getWalWriter(shard).appendBatch(walEntries);
  }
}

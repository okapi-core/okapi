/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.service.MetricsStreamIdentifier;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.queryproc.FanoutGrouper;
import org.okapi.spring.configs.Qualifiers;
import org.springframework.beans.factory.annotation.Qualifier;

public class PeerMetricsQp implements MetricsQueryProcessor {
  MetricsCfg cfg;
  FanoutGrouper fanoutGrouper;
  MetricsClient client;
  Duration perNodeTimeout;
  ExecutorService executorService;

  public PeerMetricsQp(
      MetricsCfg cfg,
      FanoutGrouper fanoutGrouper,
      MetricsClient client,
      @Qualifier(Qualifiers.METRICS_PEER_QUERY_TIMEOUT) Duration perNodeTimeout,
      @Qualifier(Qualifiers.EXEC_PEER_METRICS) ExecutorService executorService) {
    this.cfg = cfg;
    this.fanoutGrouper = fanoutGrouper;
    this.client = client;
    this.perNodeTimeout = perNodeTimeout;
    this.executorService = executorService;
  }

  @Override
  public List<TimestampedReadonlySketch> getGaugeSketches(
      String metricName, Map<String, String> paths, RES_TYPE resType, long startTime, long endTime)
      throws Exception {
    var localPath = MetricPaths.localPath(metricName, paths);
    var streamIdentifier = new MetricsStreamIdentifier(localPath);
    var idxExpiryDurationMs = cfg.getIdxExpiryDuration();
    var blockStart = startTime / cfg.getIdxExpiryDuration();
    var blockEnd = endTime / cfg.getIdxExpiryDuration();
    var groupedPaths =
        fanoutGrouper.getQueryBoundariesPerNode(
            streamIdentifier, blockStart, blockEnd, idxExpiryDurationMs);
    var suppliers = new ArrayList<Supplier<List<TimestampedReadonlySketch>>>();
    for (var entry : groupedPaths.keySet()) {
      final var nodeId = entry;
      var memberRanges = groupedPaths.get(entry);
      for (var range : memberRanges) {
        var qStart = Math.max(startTime, range[0]);
        var qEnd = Math.min(endTime, range[1]);
        suppliers.add(
            () -> {
              return client.queryGaugeSketches(nodeId, metricName, paths, resType, qStart, qEnd);
            });
      }
    }
    var aggregator = new ParrallelAggregator<TimestampedReadonlySketch>(executorService);
    return aggregator.aggregate(suppliers, perNodeTimeout);
  }

  @Override
  public List<ReadonlyHistogram> getHistograms(
      String metricName, Map<String, String> paths, long startTime, long endTime) throws Exception {
    var idxExpiryDurationMs = cfg.getIdxExpiryDuration();
    var localPath = MetricPaths.localPath(metricName, paths);
    var streamIdentifier = new MetricsStreamIdentifier(localPath);
    var blockStart = startTime / idxExpiryDurationMs;
    var blockEnd = endTime / idxExpiryDurationMs;
    var groupedPaths =
        fanoutGrouper.getQueryBoundariesPerNode(
            streamIdentifier, blockStart, blockEnd, idxExpiryDurationMs);
    var suppliers = new ArrayList<Supplier<List<ReadonlyHistogram>>>();
    for (var entry : groupedPaths.keySet()) {
      final var nodeId = entry;
      var memberRanges = groupedPaths.get(entry);
      for (var range : memberRanges) {
        var qStart = Math.max(startTime, range[0]);
        var qEnd = Math.min(endTime, range[1]);
        suppliers.add(() -> client.queryHistograms(nodeId, metricName, paths, qStart, qEnd));
      }
    }
    var aggregator = new ParrallelAggregator<ReadonlyHistogram>(executorService);
    return aggregator.aggregate(suppliers, perNodeTimeout);
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import com.google.inject.Inject;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.okapi.CommonConfig;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.byterange.DiskByteRangeSupplier;
import org.okapi.intervals.IntervalUtils;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.service.*;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;

@Slf4j
public class OnDiskMetricsQueryProcessor implements MetricsQueryProcessor {

  MetricsCfg cfg;
  DiskLogBinPaths<String> diskLogBinPaths;
  MetricsByteRangeQp metricsByteRangeQp;

  @Inject
  public OnDiskMetricsQueryProcessor(
      MetricsCfg cfg, MetricsPageCodec pageCodec, DiskLogBinPaths<String> binPaths) {
    this.cfg = cfg;
    this.diskLogBinPaths = binPaths;
    this.metricsByteRangeQp = new MetricsByteRangeQp(pageCodec);
  }

  @Override
  public List<TimestampedReadonlySketch> getGaugeSketches(
      String metricName, Map<String, String> paths, RES_TYPE resType, long startTime, long endTime)
      throws Exception {

    var gauges = new ArrayList<TimestampedReadonlySketch>();
    var modifiedStart = IntervalUtils.alignedStart(endTime, cfg.getIdxExpiryDuration());
    var matchingPaths =
        diskLogBinPaths.listLogBinFiles(
            new MetricsStreamIdentifier(CommonConfig.METRICS_STREAM_TYPE), modifiedStart, endTime);
    for (var path : matchingPaths) {

      if (!Files.exists(path)) {
        continue;
      }
      try (var byteSupplier = new DiskByteRangeSupplier(path); ) {
        gauges.addAll(
            metricsByteRangeQp.getGauges(
                metricName, paths, resType, startTime, endTime, byteSupplier));
      }
    }
    return gauges;
  }

  @Override
  public List<ReadonlyHistogram> getHistograms(
      String metricName, Map<String, String> paths, long startTime, long endTime) throws Exception {
    var histograms = new ArrayList<ReadonlyHistogram>();
    for (var path :
        diskLogBinPaths.listLogBinFiles(
            new MetricsStreamIdentifier(CommonConfig.METRICS_STREAM_TYPE), startTime, endTime)) {
      if (!Files.exists(path)) {
        continue;
      }
      try (var byteSupplier = new DiskByteRangeSupplier(path); ) {
        histograms.addAll(
            metricsByteRangeQp.getHistograms(metricName, paths, startTime, endTime, byteSupplier));
      }
    }
    return histograms;
  }
}

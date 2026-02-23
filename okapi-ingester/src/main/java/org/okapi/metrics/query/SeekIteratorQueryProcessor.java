/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.okapi.byterange.RangeIterationException;
import org.okapi.intervals.IntervalUtils;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.primitives.GaugeBlock;
import org.okapi.metrics.primitives.HistoBlock;
import org.okapi.pages.BlockSeekIterator;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.protos.metrics.METRIC_TYPE;

@Slf4j
public class SeekIteratorQueryProcessor {
  public static final double[] PERCENTILES = new double[] {0.5, 0.75, 0.9, 0.99};

  BlockSeekIterator seekIterator;
  MetricsPageCodec codec;

  public SeekIteratorQueryProcessor(BlockSeekIterator seekIterator, MetricsPageCodec codec) {
    this.seekIterator = seekIterator;
    this.codec = codec;
  }

  public List<TimestampedReadonlySketch> getGaugeSketches(
      String metricName, Map<String, String> paths, RES_TYPE resType, long startTime, long endTime)
      throws RangeIterationException, StreamReadingException, IOException, NotEnoughBytesException {

    var sketches = new ArrayList<TimestampedReadonlySketch>();
    var localpath = MetricPaths.localPath(metricName, paths);
    var blockIterator =
        new BlockIterator<>(
            localpath,
            seekIterator,
            offsetAndLen -> offsetAndLen.getMetricType() == METRIC_TYPE.METRIC_TYPE_GAUGE,
            metadata ->
                metadata.maybeContainsPath(metricName, paths)
                    && IntervalUtils.isOverlapping(
                        startTime, endTime, metadata.getTsStart(), metadata.getTsEnd()),
            GaugeBlock::new,
            codec);
    while (blockIterator.hasMore()) {
      var maybeGaugeBlock = blockIterator.next();
      if (maybeGaugeBlock.isEmpty()) {
        continue;
      }
      var gaugeBlock = maybeGaugeBlock.get();
      var aggregated = aggregateSketches(gaugeBlock, resType, startTime, endTime);
      sketches.addAll(aggregated);
    }
    return sketches;
  }

  protected List<TimestampedReadonlySketch> aggregateSketches(
      GaugeBlock block, RES_TYPE resType, long startTime, long endTime) {
    var aggregatedSketches = new ArrayList<TimestampedReadonlySketch>();
    var blockLen =
        switch (resType) {
          case SECONDLY -> 1000L;
          case MINUTELY -> 60_000L;
          case HOURLY -> 3_600_000L;
        };
    var startBlock = startTime / blockLen;
    var endBlock = endTime / blockLen;
    for (var blk = startBlock; blk <= endBlock; blk++) {
      var sketchOpt = block.getStat(blk, resType, PERCENTILES);
      final var blockTs = blk * blockLen;
      sketchOpt.ifPresent(sk -> aggregatedSketches.add(new TimestampedReadonlySketch(blockTs, sk)));
    }
    return aggregatedSketches;
  }

  public List<ReadonlyHistogram> getHistograms(
      String metricName, Map<String, String> paths, long startTime, long endTime)
      throws StreamReadingException, IOException, RangeIterationException, NotEnoughBytesException {

    var sketches = new ArrayList<ReadonlyHistogram>();
    var localpath = MetricPaths.localPath(metricName, paths);
    var blockIterator =
        new BlockIterator<>(
            localpath,
            seekIterator,
            offsetAndLen -> offsetAndLen.getMetricType() == METRIC_TYPE.METRIC_TYPE_HISTOGRAM,
            metadata ->
                metadata.maybeContainsPath(metricName, paths)
                    && IntervalUtils.isOverlapping(
                        startTime, endTime, metadata.getTsStart(), metadata.getTsEnd()),
            HistoBlock::new,
            codec);
    while (blockIterator.hasMore()) {
      var optionalHistoBlock = blockIterator.next();
      if (optionalHistoBlock.isEmpty()) {
        continue;
      }
      var histoBlock = optionalHistoBlock.get();
      var overlapping = histoBlock.getAllInRange(startTime, endTime);
      sketches.addAll(overlapping);
    }
    return sketches;
  }
}

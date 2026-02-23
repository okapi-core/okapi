/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.extern.slf4j.Slf4j;
import org.okapi.CommonConfig;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.byterange.S3ByteRangeSupplier;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.service.*;
import org.okapi.primitives.ReadonlyHistogram;
import org.okapi.primitives.TimestampedReadonlySketch;
import org.okapi.s3.S3ByteRangeCache;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class S3MetricsQp implements MetricsQueryProcessor {

  MetricsCfg cfg;
  BinFilesPrefixRegistry prefixRegistry;
  S3Client s3Client;
  S3ByteRangeCache s3ByteRangeCache;
  MetricsByteRangeQp metricsByteRangeQp;

  public S3MetricsQp(
      MetricsPageCodec metricsPageCodec,
      MetricsCfg cfg,
      BinFilesPrefixRegistry prefixRegistry,
      S3Client s3Client,
      S3ByteRangeCache s3ByteRangeCache) {
    this.metricsByteRangeQp = new MetricsByteRangeQp(metricsPageCodec);
    this.cfg = cfg;
    this.prefixRegistry = prefixRegistry;
    this.s3Client = s3Client;
    this.s3ByteRangeCache = s3ByteRangeCache;
  }

  public List<String> getMatchingPartitions(String logStream, long start, long end) {
    var matchingPrefixes = new ArrayList<String>();
    for (long ts = start; ts <= end; ts += cfg.getIdxExpiryDuration()) {
      var part = ts / cfg.getIdxExpiryDuration();
      var prefixes =
          prefixRegistry.getAllPrefixesForLogBinFile(
              cfg.getS3Bucket(),
              cfg.getS3BasePrefix(),
              logStream,
              PartNames.METRICS_FILE_PART,
              part);
      matchingPrefixes.addAll(prefixes);
    }
    return matchingPrefixes;
  }

  @Override
  public List<TimestampedReadonlySketch> getGaugeSketches(
      String name, Map<String, String> tags, RES_TYPE resType, long startTime, long endTime)
      throws Exception {
    var prefixes = getMatchingPartitions(CommonConfig.METRICS_STREAM_TYPE, startTime, endTime);
    var gauges = new ConcurrentLinkedQueue<List<TimestampedReadonlySketch>>();
    prefixes.stream()
        .parallel()
        .forEach(
            prefix -> {
              log.info("Querying S3 prefix {}", prefix);
              var s3ByteRangeSupplier =
                  new S3ByteRangeSupplier(cfg.getS3Bucket(), prefix, s3Client, s3ByteRangeCache);
              try {
                gauges.add(
                    metricsByteRangeQp.getGauges(
                        name, tags, resType, startTime, endTime, s3ByteRangeSupplier));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    var gaugeResults = new ArrayList<TimestampedReadonlySketch>();
    for (var gaugeResult : gauges) {
      gaugeResults.addAll(gaugeResult);
    }
    return gaugeResults;
  }

  @Override
  public List<ReadonlyHistogram> getHistograms(
      String name, Map<String, String> tags, long startTime, long endTime) throws Exception {
    var prefixes = getMatchingPartitions(CommonConfig.METRICS_STREAM_TYPE, startTime, endTime);
    var histos = new ConcurrentLinkedQueue<List<ReadonlyHistogram>>();
    prefixes.stream()
        .parallel()
        .forEach(
            prefix -> {
              var s3ByteRangeSupplier =
                  new S3ByteRangeSupplier(cfg.getS3Bucket(), prefix, s3Client, s3ByteRangeCache);
              try {
                histos.add(
                    metricsByteRangeQp.getHistograms(
                        name, tags, startTime, endTime, s3ByteRangeSupplier));
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
            });
    var histoResults = new ArrayList<ReadonlyHistogram>();
    for (var histoResult : histos) {
      histoResults.addAll(histoResult);
    }
    return histoResults;
  }
}

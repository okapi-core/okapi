/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.service.GaugeAggregator;
import org.okapi.rest.metrics.query.GetGaugeResponse;
import org.okapi.s3.S3ByteRangeCache;
import org.okapi.testutils.OkapiTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3MetricsQpTests {

  static final String BUCKET = "okapi-metrics-s3-qp-tests";

  @BeforeAll
  static void setupAll() {
    var client = OkapiTestUtils.getLocalstackS3Client();
    client.createBucket(b -> b.bucket(BUCKET));
  }

  private static String newTenant() {
    return UUID.randomUUID().toString();
  }

  private static void put(S3Client s3, String bucket, String key, byte[] data) {
    s3.putObject(
        PutObjectRequest.builder().bucket(bucket).key(key).build(), RequestBody.fromBytes(data));
  }

  private static S3ByteRangeCache s3ByteRangeCache() {
    return new S3ByteRangeCache(1024L);
  }

  private static GetGaugeResponse gaugeResponse(
      S3MetricsQp qp,
      String metric,
      Map<String, String> tags,
      long start,
      long end,
      RES_TYPE res,
      AGG_TYPE agg)
      throws Exception {
    var sketches = qp.getGaugeSketches(metric, tags, res, start, end);
    return GaugeAggregator.aggregateSketches(sketches, res, agg);
  }

  private static S3MetricsQp qpForTenantWithKeys(List<String> keys) {
    MetricsCfg cfg = mock(MetricsCfg.class);
    when(cfg.getS3Bucket()).thenReturn(BUCKET);
    when(cfg.getS3BasePrefix()).thenReturn("metrics");
    when(cfg.getIdxExpiryDuration()).thenReturn(10_000_000L);

    BinFilesPrefixRegistry registry = mock(BinFilesPrefixRegistry.class);
    when(registry.getAllPrefixesForLogBinFile(
            eq(BUCKET), eq("metrics"), eq("metrics"), eq(PartNames.METRICS_FILE_PART), anyLong()))
        .thenReturn(keys);

    var s3Client = OkapiTestUtils.getLocalstackS3Client();
    var cache = s3ByteRangeCache();
    return new S3MetricsQp(new MetricsPageCodec(), cfg, registry, s3Client, cache);
  }

  private static List<Long> sortedTimes(
      S3MetricsQp qp,
      String metric,
      Map<String, String> tags,
      long start,
      long end,
      RES_TYPE res,
      AGG_TYPE agg)
      throws Exception {
    var gr = gaugeResponse(qp, metric, tags, start, end, res, agg);
    return gr.getTimes().stream().sorted().collect(Collectors.toList());
  }

  private static List<Float> valuesSortedByTimes(
      S3MetricsQp qp,
      String metric,
      Map<String, String> tags,
      long start,
      long end,
      RES_TYPE res,
      AGG_TYPE agg)
      throws Exception {
    var gr = gaugeResponse(qp, metric, tags, start, end, res, agg);
    var pairs = new ArrayList<Map.Entry<Long, Float>>();
    for (int i = 0; i < gr.getTimes().size(); i++) {
      pairs.add(Map.entry(gr.getTimes().get(i), gr.getValues().get(i)));
    }
    pairs.sort(Comparator.comparingLong(Map.Entry::getKey));
    return pairs.stream().map(Map.Entry::getValue).collect(Collectors.toList());
  }

  @Test
  void testSingleObjectSecondlyAvg() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/" + "/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    assertEquals(
        List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
    assertEquals(
        List.of(0.625f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
  }

  @Test
  void testMultipleObjectsInterleaved_sorted() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var a = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    var b = "metrics/metrics/0/nodeB/" + PartNames.METRICS_FILE_PART;
    var c = "metrics/metrics/0/nodeC/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, a, SampleMetricsPages.getMetricsGaugePage1());
    put(s3, BUCKET, b, SampleMetricsPages.getMetricsGaugePage3()); // different metric
    put(s3, BUCKET, c, SampleMetricsPages.getMetricsGaugePage2());

    var qp = qpForTenantWithKeys(List.of(a, b, c));
    assertEquals(
        List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
    assertEquals(
        List.of(0.625f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
  }

  @Test
  void testDifferentMetricsPresent_filters() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getPagesWithDifferentMetrics());

    var qp = qpForTenantWithKeys(List.of(key));
    assertEquals(
        List.of(1000L, 2000L, 4000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            8000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
    assertEquals(
        List.of(0.625f, 0.9f, 0.6f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            8000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
  }

  @Test
  void testTagSuperset_noResults() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    var gr =
        gaugeResponse(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS_SUPERSET,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);
    assertTrue(gr.getTimes().isEmpty());
    assertTrue(gr.getValues().isEmpty());
  }

  @Test
  void testBoundary_endJustBeforeBoundary() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    assertEquals(
        List.of(1000L, 2000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            2999L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
    assertEquals(
        List.of(0.625f, 0.9f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            2999L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
  }

  @Test
  void testSinglePointInclusionWindow() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    assertEquals(
        List.of(5000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            5000L,
            5001L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
    assertEquals(
        List.of(0.65f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            5000L,
            5001L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG));
  }

  @Test
  void testMinutelyAvg_twoSketches() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    var gr =
        gaugeResponse(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.MINUTELY,
            AGG_TYPE.AVG);
    assertEquals(1, gr.getTimes().size());
    // both timestamps are 0 (minute 0); order of values may vary
    var values = new HashSet<>(gr.getValues());
    assertEquals(1, values.size());
    assertTrue(values.contains(0.6875f));
  }

  @Test
  void testSecondlySum() throws Exception {
    var s3 = OkapiTestUtils.getLocalstackS3Client();
    var key = "metrics/metrics/0/nodeA/" + PartNames.METRICS_FILE_PART;
    put(s3, BUCKET, key, SampleMetricsPages.getGaugePage());

    var qp = qpForTenantWithKeys(List.of(key));
    assertEquals(
        List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L),
        sortedTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.SUM));
    assertEquals(
        List.of(1.25f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f),
        valuesSortedByTimes(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.SUM));
  }
}

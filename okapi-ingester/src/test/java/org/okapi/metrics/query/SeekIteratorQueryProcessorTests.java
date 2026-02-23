/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.okapi.byterange.LengthPrefixedBlockSeekIterator;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.primitives.TimestampedReadonlySketch;

public class SeekIteratorQueryProcessorTests {

  private SeekIteratorQueryProcessor qpForBytes(byte[] bytes) {
    var seekIterator =
        new LengthPrefixedBlockSeekIterator(SampleMetricsPages.getRangeSupplier(bytes));
    var codec = new MetricsPageCodec();
    return new SeekIteratorQueryProcessor(seekIterator, codec);
  }

  private static float sumCounts(List<TimestampedReadonlySketch> sketches) {
    float total = 0f;
    for (var s : sketches) {
      total += s.getReadOnlySketch().getCount();
    }
    return total;
  }

  private static boolean containsMean(
      List<TimestampedReadonlySketch> sketches, float target, float eps) {
    for (var s : sketches) {
      if (Math.abs(s.getReadOnlySketch().getMean() - target) <= eps) return true;
    }
    return false;
  }

  @Test
  void testSimpleScanQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            6000L);

    // Expect seconds: 1,2,3,4,5,6 -> 6 sketches
    assertEquals(6, sketches.size());
    // Total samples: 7 (2 in sec=1, then 1 each in 2,3,4,5,6)
    assertEquals(7f, sumCounts(sketches), 1e-3);
    // Means present (with tolerance)
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
    assertTrue(containsMean(sketches, 0.8f, 1e-3f));
    assertTrue(containsMean(sketches, 0.6f, 1e-3f));
    assertTrue(containsMean(sketches, 0.65f, 1e-3f));
    assertTrue(containsMean(sketches, 0.7f, 1e-3f));
  }

  @Test
  void testOverlappingScanQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1500L,
            3500L);

    // Expect seconds: 1,2,3 -> 3 sketches
    assertEquals(3, sketches.size());
    // Total samples: 4 (2 in sec=1, then 1 each in 2 and 3)
    assertEquals(4f, sumCounts(sketches), 1e-3);
    // Means present
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
    assertTrue(containsMean(sketches, 0.8f, 1e-3f));
  }

  @Test
  void testEmptyQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            700L,
            999L);

    assertTrue(sketches.isEmpty());
  }

  @Test
  void testNonExistingMetricQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            "does_not_exist", SampleMetricsPages.METRIC_TAGS, RES_TYPE.SECONDLY, 0L, 10_000L);

    assertTrue(sketches.isEmpty());
  }

  @Test
  void testTagOnlyQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    // Use a subset of tags; path won't match exactly -> no results
    Map<String, String> partialTags = Map.of("host", "server1");
    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME, partialTags, RES_TYPE.SECONDLY, 1000L, 6000L);

    assertTrue(sketches.isEmpty());
  }

  @Test
  void testInterleavedPagesScanQuery()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getPagesWithMetricsApart();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            6000L);

    // Should behave same as simple scan across two relevant pages
    assertEquals(6, sketches.size());
    assertEquals(7f, sumCounts(sketches), 1e-3);
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
    assertTrue(containsMean(sketches, 0.8f, 1e-3f));
    assertTrue(containsMean(sketches, 0.6f, 1e-3f));
    assertTrue(containsMean(sketches, 0.65f, 1e-3f));
    assertTrue(containsMean(sketches, 0.7f, 1e-3f));
  }

  @Test
  void testDifferentMetricsPresent()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getPagesWithDifferentMetrics();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            8000L);

    // Only the first page contributes
    assertEquals(3, sketches.size());
    assertEquals(4.0f, sumCounts(sketches), 1e-3);
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
    assertTrue(containsMean(sketches, 0.6f, 1e-3f));
    // Means from page 2 (different metric) must not appear
    assertFalse(containsMean(sketches, 0.55f, 1e-3f));
    assertFalse(containsMean(sketches, 0.45f, 1e-3f));
  }

  @Test
  void testBoundarySemantics_endJustBeforeBoundary()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            2999L);

    // Should include seconds 1 and 2
    assertEquals(2, sketches.size());
    assertEquals(3f, sumCounts(sketches), 1e-3);
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
  }

  @Test
  void testBoundarySemantics_endExactlyOnBoundary()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            3001L);

    // Should include seconds 1,2,3 (inclusive on end block)
    assertEquals(3, sketches.size());
    assertEquals(4f, sumCounts(sketches), 1e-3);
    assertTrue(containsMean(sketches, 0.625f, 1e-3f));
    assertTrue(containsMean(sketches, 0.9f, 1e-3f));
    assertTrue(containsMean(sketches, 0.8f, 1e-3f));
  }

  @Test
  void testSinglePointWindow()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            3000L,
            4001L);

    assertEquals(2, sketches.size());
    assertEquals(2f, sumCounts(sketches), 1e-3);
    assertTrue(containsMean(sketches, 0.6f, 1e-3f));
    assertTrue(containsMean(sketches, 0.8f, 1e-3f));
  }

  @Test
  void testMinutelyAggregation_twoPages()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.MINUTELY,
            1000L,
            6000L);

    // Expect one minute block (0) per page -> two sketches
    assertEquals(2, sketches.size());
    // Verify we have counts 4 and 3 present (order not assumed)
    boolean saw4 = false, saw3 = false;
    boolean sawMeanA = false, sawMeanB = false;
    for (var s : sketches) {
      if (Math.abs(s.getReadOnlySketch().getCount() - 4f) <= 1e-3) saw4 = true;
      if (Math.abs(s.getReadOnlySketch().getCount() - 3f) <= 1e-3) saw3 = true;
      if (Math.abs(s.getReadOnlySketch().getMean() - 0.6875f) <= 1e-3)
        sawMeanA = true; // (0.5+0.75+0.9+0.6)/4
      if (Math.abs(s.getReadOnlySketch().getMean() - (2.15f / 3f)) <= 1e-3)
        sawMeanB = true; // (0.8+0.65+0.7)/3
    }
    assertTrue(saw4 && saw3);
    assertTrue(sawMeanA && sawMeanB);
  }

  @Test
  void testMinutelyAggregation_narrowWindowFirstPageOnly()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.MINUTELY,
            0L,
            2000L);

    // Only the first page overlaps -> one minute sketch for minute 0
    assertEquals(1, sketches.size());
    assertEquals(4f, sketches.get(0).getReadOnlySketch().getCount(), 1e-3);
    assertEquals(0.6875f, sketches.get(0).getReadOnlySketch().getMean(), 1e-3);
  }

  @Test
  void testHourlyAggregation_twoPages()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.HOURLY,
            1000L,
            6000L);

    // Expect one hourly block (0) per page -> two sketches
    assertEquals(2, sketches.size());
    boolean saw4 = false, saw3 = false;
    boolean sawMeanA = false, sawMeanB = false;
    for (var s : sketches) {
      if (Math.abs(s.getReadOnlySketch().getCount() - 4f) <= 1e-3) saw4 = true;
      if (Math.abs(s.getReadOnlySketch().getCount() - 3f) <= 1e-3) saw3 = true;
      if (Math.abs(s.getReadOnlySketch().getMean() - 0.6875f) <= 1e-3) sawMeanA = true;
      if (Math.abs(s.getReadOnlySketch().getMean() - (2.15f / 3f)) <= 1e-3) sawMeanB = true;
    }
    assertTrue(saw4 && saw3);
    assertTrue(sawMeanA && sawMeanB);
  }

  @Test
  void testSupersetTags_noResults()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var supersetTags = new java.util.TreeMap<>(SampleMetricsPages.METRIC_TAGS);
    supersetTags.put("env", "prod");

    var sketches =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME, supersetTags, RES_TYPE.SECONDLY, 1000L, 6000L);

    assertTrue(sketches.isEmpty());
  }

  @Test
  void testReusingProcessorReturnsEmptyOnSecondCall()
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var bytes = SampleMetricsPages.getGaugePage();
    var qp = qpForBytes(bytes);

    var first =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            6000L);
    assertEquals(6, first.size());

    var second =
        qp.getGaugeSketches(
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            RES_TYPE.SECONDLY,
            1000L,
            6000L);
    assertTrue(second.isEmpty());
  }
}

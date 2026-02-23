package org.okapi.metrics.query;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.metrics.paths.MetricsDiskPaths;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.service.GaugeAggregator;
import org.okapi.rest.metrics.query.GetGaugeResponse;

public class OnDiskMetricsQueryProcessorTests {

  private OnDiskMetricsQueryProcessor qpWithPaths(List<Path> paths) {
    var binPaths = mock(MetricsDiskPaths.class);
    when(binPaths.listLogBinFiles(any(), anyLong(), anyLong())).thenReturn(paths);
    var cfg = mock(MetricsCfg.class);
    when(cfg.getIdxExpiryDuration()).thenReturn(3600000L); // 1 hour
    return new OnDiskMetricsQueryProcessor(cfg, new MetricsPageCodec(), binPaths);
  }

  private static GetGaugeResponse resp(
      OnDiskMetricsQueryProcessor qp,
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

  private static List<Long> sortedTimes(GetGaugeResponse gr) {
    return gr.getTimes().stream().sorted().collect(Collectors.toList());
  }

  private static List<Float> valuesSortedByTimes(GetGaugeResponse gr) {
    var pairs = new ArrayList<Map.Entry<Long, Float>>();
    for (int i = 0; i < gr.getTimes().size(); i++) {
      pairs.add(Map.entry(gr.getTimes().get(i), gr.getValues().get(i)));
    }
    pairs.sort(Comparator.comparingLong(Map.Entry::getKey));
    return pairs.stream().map(Map.Entry::getValue).collect(Collectors.toList());
  }

  @Test
  void testSingleFileSecondlyAvg_sorted() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertEquals(List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L), sortedTimes(gr));
    assertEquals(List.of(0.625f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f), valuesSortedByTimes(gr));
  }

  @Test
  void testSingleFileMinutelyAvg_twoSketches() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.MINUTELY,
            AGG_TYPE.AVG);

    assertEquals(1, gr.getTimes().size());
    assertEquals(0.6875f, gr.getValues().get(0));
  }

  @Test
  void testDifferentMetricsPresent_filtersOutOthers() throws Exception {
    var file =
        SampleMetricsPages.writeToTempFile(SampleMetricsPages.getPagesWithDifferentMetrics());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            8000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertEquals(List.of(1000L, 2000L, 4000L), sortedTimes(gr));
    assertEquals(List.of(0.625f, 0.9f, 0.6f), valuesSortedByTimes(gr));
  }

  @Test
  void testInterleavedAcrossMultipleFiles_sorted() throws Exception {
    var a = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getMetricsGaugePage1());
    var b =
        SampleMetricsPages.writeToTempFile(
            SampleMetricsPages.getMetricsGaugePage3()); // different metric
    var c = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getMetricsGaugePage2());
    var qp = qpWithPaths(List.of(a, b, c));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertEquals(List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L), sortedTimes(gr));
    assertEquals(List.of(0.625f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f), valuesSortedByTimes(gr));
  }

  @Test
  void testTagSupersetNoResults() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
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
  void testNonexistentPathIgnored() throws Exception {
    var real = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var fake = Path.of("/does/not/exist.bin");
    var qp = qpWithPaths(List.of(fake, real));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertValuesMatch(
        gr,
        Map.of(1000L, 0.625f, 2000L, 0.9f, 3000L, 0.8f, 4000L, 0.6f, 5000L, 0.65f, 6000L, 0.7f));
  }

  @Test
  void testBoundary_endJustBeforeBoundary() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            2999L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertEquals(List.of(1000L, 2000L), sortedTimes(gr));
    assertEquals(List.of(0.625f, 0.9f), valuesSortedByTimes(gr));
  }

  @Test
  void testSinglePointInclusionWindow() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            5000L,
            5001L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.AVG);

    assertEquals(List.of(5000L), sortedTimes(gr));
    assertEquals(List.of(0.65f), valuesSortedByTimes(gr));
  }

  @Test
  void testAggregationSum() throws Exception {
    var file = SampleMetricsPages.writeToTempFile(SampleMetricsPages.getGaugePage());
    var qp = qpWithPaths(List.of(file));

    var gr =
        resp(
            qp,
            SampleMetricsPages.METRIC_NAME,
            SampleMetricsPages.METRIC_TAGS,
            1000L,
            6000L,
            RES_TYPE.SECONDLY,
            AGG_TYPE.SUM);

    assertEquals(List.of(1000L, 2000L, 3000L, 4000L, 5000L, 6000L), sortedTimes(gr));
    assertEquals(List.of(1.25f, 0.9f, 0.8f, 0.6f, 0.65f, 0.7f), valuesSortedByTimes(gr));
  }

  public void assertValuesMatch(GetGaugeResponse response, Map<Long, Float> ref) {
    var mapped = mapFromResponse(response);
    assertEquals(ref.size(), mapped.size());
    for (var entry : ref.entrySet()) {
      assertTrue(mapped.containsKey(entry.getKey()), "Missing key: " + entry.getKey());
      assertEquals(
          entry.getValue(),
          mapped.get(entry.getKey()),
          "Value mismatch for key: "
              + entry.getKey()
              + " expected "
              + entry.getValue()
              + " but got "
              + mapped.get(entry.getKey()));
    }
  }

  public Map<Long, Float> mapFromResponse(GetGaugeResponse response) {
    Map<Long, Float> result = new HashMap<>();
    for (int i = 0; i < response.getTimes().size(); i++) {
      result.put(response.getTimes().get(i), response.getValues().get(i));
    }
    return result;
  }
}

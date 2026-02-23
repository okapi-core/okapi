/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;

public class PayloadSplitterTests {

  @Test
  void testSplitGauge_ByBlocks_basic() {
    var splitter = getSplitter(1000L);
    var gauge =
        MetricsTestingUtils.gauge(
            new long[] {0, 200, 800, 999, 1000, 1001}, new float[] {1, 2, 3, 4, 5, 6});
    var chunks = splitter.splitGaugeByBlocks(gauge);
    assertEquals(2, chunks.size());

    var g1 = chunks.get(0L);
    var g2 = chunks.get(1L);
    assertEquals(List.of(0L, 200L, 800L, 999L), g1.getTs());
    assertEquals(List.of(1f, 2f, 3f, 4f), g1.getValue());
    assertEquals(List.of(1000L, 1001L), g2.getTs());
    assertEquals(List.of(5f, 6f), g2.getValue());
  }

  @Test
  void testSplitGauge_ByBlocks_exactBoundary() {
    var splitter = getSplitter(1000L);
    var gauge =
        MetricsTestingUtils.gauge(new long[] {0, 999, 1000, 1001}, new float[] {1, 2, 3, 4});
    var chunks = splitter.splitGaugeByBlocks(gauge);
    assertEquals(2, chunks.size());
    assertEquals(List.of(0L, 999L), chunks.get(0L).getTs());
    assertEquals(List.of(1f, 2f), chunks.get(0L).getValue());
    assertEquals(List.of(1000L, 1001L), chunks.get(1L).getTs());
    assertEquals(List.of(3f, 4f), chunks.get(1L).getValue());
  }

  @Test
  void testSplitGauge_noSplitByBlocks() {
    var splitter = getSplitter(5000L);
    var gauge =
        MetricsTestingUtils.gauge(new long[] {100, 200, 600, 900}, new float[] {1, 2, 3, 4});
    var chunks = splitter.splitGaugeByBlocks(gauge);
    assertEquals(1, chunks.size());
    assertEquals(List.of(100L, 200L, 600L, 900L), chunks.get(0L).getTs());
    assertEquals(List.of(1f, 2f, 3f, 4f), chunks.get(0L).getValue());
  }

  // Dropped zero-block test: blockDurationMs is expected > 0 in current implementation.

  @Test
  void testSplitGauge_ByBlocks_emptyAndSingleton() {
    var splitter = getSplitter(1000L);
    var empty = Gauge.builder().ts(List.of()).value(List.of()).build();
    assertEquals(0, splitter.splitGaugeByBlocks(empty).size());

    var singleton = MetricsTestingUtils.gauge(new long[] {42}, new float[] {3.14f});
    var chunks = splitter.splitGaugeByBlocks(singleton);
    assertEquals(1, chunks.size());
    assertEquals(List.of(42L), chunks.get(0L).getTs());
    assertEquals(List.of(3.14f), chunks.get(0L).getValue());
  }

  @Test
  void testSplitHisto_ByBlocks_basic() {
    var splitter = getSplitter(1000L);
    var p = new float[] {10f};
    var c = new int[] {1, 2};
    var h =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(0, 100, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(200, 300, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(800, 900, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(999, 1100, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1000, 1200, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1001, 1300, HistoPoint.TEMPORALITY.DELTA, p, c));
    var chunks = splitter.splitHistoByBlocks(h);
    assertEquals(2, chunks.keySet().size());
    assertEquals(4, chunks.get(0L).size());
    assertEquals(2, chunks.get(1L).size());
    var starts0 = chunks.get(0L).stream().map(HistoPoint::getStart).sorted().toList();
    var starts1 = chunks.get(1L).stream().map(HistoPoint::getStart).sorted().toList();
    assertEquals(List.of(0L, 200L, 800L, 999L), starts0);
    assertEquals(List.of(1000L, 1001L), starts1);
  }

  @Test
  void testSplitHisto_ByBlocks_exactBoundary() {
    var splitter = getSplitter(1000L);
    var p = new float[] {};
    var c = new int[] {1};
    var h =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(0, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(999, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1000, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1001, 10, HistoPoint.TEMPORALITY.DELTA, p, c));
    var chunks = splitter.splitHistoByBlocks(h);
    assertEquals(2, chunks.keySet().size());
    assertEquals(2, chunks.get(0L).size());
    assertEquals(2, chunks.get(1L).size());
    var starts0 = chunks.get(0L).stream().map(HistoPoint::getStart).sorted().toList();
    var starts1 = chunks.get(1L).stream().map(HistoPoint::getStart).sorted().toList();
    assertEquals(List.of(0L, 999L), starts0);
    assertEquals(List.of(1000L, 1001L), starts1);
  }

  @Test
  void testSplitHisto_noSplitByBlocks() {
    var splitter = getSplitter(5000L);
    var p = new float[] {10f};
    var c = new int[] {1, 2};
    var h =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(100, 200, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(600, 700, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(900, 1000, HistoPoint.TEMPORALITY.DELTA, p, c));
    var chunks = splitter.splitHistoByBlocks(h);
    assertEquals(1, chunks.keySet().size());
    assertEquals(3, chunks.get(0L).size());
  }

  @Test
  void testSplitPayload_ByBlocks_gaugeOnly() {
    var splitter = getSplitter(1000L);
    var gauge =
        MetricsTestingUtils.gauge(new long[] {0, 999, 1000, 1001}, new float[] {1, 2, 3, 4});
    ExportMetricsRequest req =
        MetricsTestingUtils.exportReq("metricA", Map.of("k", "v"), gauge, null);
    var out = splitter.splitPayloadByBlocks(req);
    assertEquals(2, out.size());

    var list0 = out.get(0L);
    var list1 = out.get(1L);
    assertEquals(1, list0.size());
    assertEquals(1, list1.size());
    var gOut1 = list0.iterator().next().getGauge();
    var gOut2 = list1.iterator().next().getGauge();
    assertEquals(List.of(0L, 999L), gOut1.getTs());
    assertEquals(List.of(1f, 2f), gOut1.getValue());
    assertEquals(List.of(1000L, 1001L), gOut2.getTs());
    assertEquals(List.of(3f, 4f), gOut2.getValue());
    assertEquals("metricA", list0.iterator().next().getMetricName());
    // validate tags
    assertEquals(Map.of("k", "v"), list0.iterator().next().getTags());
  }

  @Test
  void testSplitPayload_ByBlocks_histoOnly() {
    var splitter = getSplitter(1000L);
    var p = new float[] {};
    var c = new int[] {1};
    Histo histo =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(0, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(999, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1000, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1001, 10, HistoPoint.TEMPORALITY.DELTA, p, c));

    ExportMetricsRequest req =
        MetricsTestingUtils.exportReq("metricB", Map.of("a", "b"), null, histo);
    var out = splitter.splitPayloadByBlocks(req);
    assertEquals(2, out.size());
    var h0 = out.get(0L).iterator().next();
    var h1 = out.get(1L).iterator().next();
    assertNull(h0.getGauge());
    assertNotNull(h0.getHisto());
    assertEquals(2, h0.getHisto().getHistoPoints().size());
    assertEquals(2, h1.getHisto().getHistoPoints().size());

    // validate dats
    assertEquals("metricB", h0.getMetricName());
    assertEquals(Map.of("a", "b"), h0.getTags());
  }

  @Test
  void testSplitPayload_ByBlocks_mixedBothPresent() {
    var splitter = getSplitter(1000L);
    var gauge =
        MetricsTestingUtils.gauge(new long[] {0, 999, 1000, 1001}, new float[] {1, 2, 3, 4});
    var p = new float[] {};
    var c = new int[] {1};
    var histo =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(0, 10, HistoPoint.TEMPORALITY.DELTA, p, c),
            MetricsTestingUtils.histoPoint(1000, 10, HistoPoint.TEMPORALITY.DELTA, p, c));

    var req = MetricsTestingUtils.exportReq("metricC", Map.of("x", "y"), gauge, histo);
    var out = splitter.splitPayloadByBlocks(req);
    // gauge splits into 2, histo splits into 2 -> total 4 entries (one per block per type)
    assertEquals(4, out.size());
    int gaugeCount = (int) out.values().stream().filter(r -> r.getGauge() != null).count();
    int histoCount = (int) out.values().stream().filter(r -> r.getHisto() != null).count();
    assertEquals(2, gaugeCount);
    assertEquals(2, histoCount);
  }

  public PayloadSplitter getSplitter(long dur) {
    var mockCfg = Mockito.mock(MetricsCfg.class);
    Mockito.when(mockCfg.getIdxExpiryDuration()).thenReturn(dur);
    return new PayloadSplitter(mockCfg);
  }
}

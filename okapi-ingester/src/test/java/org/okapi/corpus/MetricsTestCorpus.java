/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.corpus;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.metrics.query.MetricsTestingUtils;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.HistoPoint;

@AllArgsConstructor
public class MetricsTestCorpus {
  long testStartMs;

  /** Returns individual ExportMetricsRequest objects to send to Kafka. */
  public List<ExportMetricsRequest> getIndividualRecords() {
    long t0 = testStartMs;
    var g1 = MetricsTestingUtils.gauge(new long[] {t0, t0 + 1}, new float[] {1.0f, 2.0f});
    var g2 = MetricsTestingUtils.gauge(new long[] {t0 + 2}, new float[] {3.0f});
    var h1 =
        MetricsTestingUtils.histo(
            MetricsTestingUtils.histoPoint(
                t0,
                t0 + 1000,
                HistoPoint.TEMPORALITY.DELTA,
                new float[] {0.1f, 1.0f},
                new int[] {1, 2, 3}));
    var r1 = MetricsTestingUtils.exportReq("m1", Map.of("host", "h1"), g1, null);
    var r2 = MetricsTestingUtils.exportReq("m2", Map.of("host", "h1"), g2, null);
    var r3 = MetricsTestingUtils.exportReq("m3", Map.of("host", "h2"), null, h1);
    return List.of(r1, r2, r3);
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;
import org.okapi.rest.metrics.payloads.Histo;
import org.okapi.rest.metrics.payloads.HistoPoint;

public final class MetricsTestingUtils {

  private MetricsTestingUtils() {}

  public static Gauge gauge(long[] ts, float[] values) {
    var valuesList = new ArrayList<Float>();
    for (var v : values) {
      valuesList.add(v);
    }
    return Gauge.builder().ts(Arrays.stream(ts).boxed().toList()).value(valuesList).build();
  }

  public static HistoPoint histoPoint(
      long start, long end, HistoPoint.TEMPORALITY temporality, float[] buckets, int[] counts) {
    return HistoPoint.builder()
        .start(start)
        .end(end)
        .temporality(temporality)
        .buckets(buckets)
        .bucketCounts(counts)
        .build();
  }

  public static Histo histo(HistoPoint... points) {
    return Histo.builder().histoPoints(List.of(points)).build();
  }

  public static ExportMetricsRequest exportReq(
      String metric, Map<String, String> tags, Gauge gauge, Histo histo) {
    return ExportMetricsRequest.builder()
        .metricName(metric)
        .tags(tags)
        .gauge(gauge)
        .histo(histo)
        .build();
  }
}

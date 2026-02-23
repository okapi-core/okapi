/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.io.MetricsPage;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.rest.metrics.ExportMetricsRequest;
import org.okapi.rest.metrics.payloads.Gauge;

public class MetricsPageCodecTests {

  @Test
  void testMetricsPageCodec() throws StreamReadingException, IOException, NotEnoughBytesException {
    var page = new MetricsPage(1000L, 1000L, 1000, 0.01);
    page.append(
        ExportMetricsRequest.builder()
            .metricName("cpu_usage")
            .gauge(Gauge.builder().ts(List.of(1000L, 2000L)).value(List.of(0.1f, 0.2f)).build())
            .type(org.okapi.rest.metrics.MetricType.GAUGE)
            .build());

    var codec = new MetricsPageCodec();
    var bytes = codec.serialize(page);
    var deserializedPageOpt = codec.deserialize(bytes);
    assertTrue(deserializedPageOpt.isPresent());
    var deserializedPage = deserializedPageOpt.get();
    assertEquals(
        0.1f,
        deserializedPage.getSecondly("cpu_usage{}", 1000L, new double[] {0.5}).get().getMean());
  }
}

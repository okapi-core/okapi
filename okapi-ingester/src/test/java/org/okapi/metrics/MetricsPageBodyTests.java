/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.metrics.io.MetricsPageBody;
import org.okapi.primitives.Histogram;

public class MetricsPageBodyTests {

  @Test
  void testPageBodyWrites_singlePoint()
      throws IOException, StreamReadingException, NotEnoughBytesException {
    var body = new MetricsPageBody();
    var gaugePath = "gaugePath";
    var now = Instant.now();
    body.updateGauge(gaugePath, now.toEpochMilli() + 10, 30.f);

    var histoPath = "histoPath";
    var histogram =
        new Histogram(
            now.toEpochMilli() + 20,
            null,
            Histogram.TEMPORALITY.DELTA,
            new int[] {5, 10, 15},
            new float[] {0.0f, 50.0f});
    body.updateHistogram(histoPath, now.toEpochMilli() + 20, histogram);

    var serializedBody = body.toChecksummedByteArray();
    var deserializedBody = new MetricsPageBody();
    deserializedBody.fromChecksummedByteArray(serializedBody, 0, serializedBody.length);

    var secondly =
        deserializedBody.getSecondly(gaugePath, now.toEpochMilli() + 10, new double[] {0.0, 1.0});
    assertTrue(secondly.isPresent());
    assertEquals(30.f, secondly.get().getMean());

    var deserializedHistoOpt = deserializedBody.getHistogram(histoPath, now.toEpochMilli() + 20);
    assertTrue(deserializedHistoOpt.isPresent());
    var deserializedHisto = deserializedHistoOpt.get();
    assertEquals(histogram.getStartTs(), deserializedHisto.getStartTs());
    assertEquals(histogram.getTemporality(), deserializedHisto.getTemporality());
    assertEquals(histogram.getBucketCounts().length, deserializedHisto.getBucketCounts().size());
    for (int i = 0; i < histogram.getBucketCounts().length; i++) {
      assertEquals(histogram.getBucketCounts()[i], deserializedHisto.getBucketCounts().get(i));
      if (i == histogram.getBucketCounts().length - 1) {
        // Last bucket is the overflow bucket, which has no corresponding bucket boundary
        continue;
      }
      assertEquals(histogram.getBuckets()[i], deserializedHisto.getBuckets().get(i));
    }
  }
}

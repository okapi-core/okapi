/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import org.junit.jupiter.api.Test;

public class HistoStatsTests {

  @Test
  public void basic() {
    var buckets = new float[] {10.f, 15.f, 19.f};
    var counts = new int[] {1, 2, 3, 1};
    var histoStats = new HistoStats(buckets, counts);
    var restored = HistoStats.deserialize(histoStats.serialize());
    assertEquals(Floats.asList(buckets), Floats.asList(restored.getBuckets()));
    assertEquals(Ints.asList(counts), Ints.asList(restored.getBucketCounts()));
  }

  @Test
  public void noBuckets() {
    var buckets = new float[] {};
    var counts = new int[] {1};
    var histoStats = new HistoStats(buckets, counts);
    var restored = HistoStats.deserialize(histoStats.serialize());
    assertEquals(Floats.asList(buckets), Floats.asList(restored.getBuckets()));
    assertEquals(Ints.asList(counts), Ints.asList(restored.getBucketCounts()));
  }

  @Test
  public void singleBucket() {
    var buckets = new float[] {20.f};
    var counts = new int[] {3, 1};
    var histoStats = new HistoStats(buckets, counts);
    var restored = HistoStats.deserialize(histoStats.serialize());
    assertEquals(Floats.asList(buckets), Floats.asList(restored.getBuckets()));
    assertEquals(Ints.asList(counts), Ints.asList(restored.getBucketCounts()));
  }
}

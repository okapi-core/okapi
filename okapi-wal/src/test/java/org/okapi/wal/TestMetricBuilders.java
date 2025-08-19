package org.okapi.wal;

import org.okapi.wal.Wal.MetricEvent;

/** Small helpers to build metric events with somewhat controllable sizes. */
public final class TestMetricBuilders {
  private TestMetricBuilders() {}

  public static MetricEvent event(String name, int pairs) {
    MetricEvent.Builder b = MetricEvent.newBuilder().setName(name);
    for (int i = 0; i < pairs; i++) {
      b.addTs(i + 1);
      b.addVals((float) (i + 1));
    }
    // a few tags to vary size
    int nTags = Math.max(0, pairs / 3);
    for (int i = 0; i < nTags; i++) {
      b.putTags("k" + i, "v" + i);
    }
    return b.build();
  }
}

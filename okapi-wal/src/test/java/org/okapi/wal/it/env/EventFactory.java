package org.okapi.wal.it.env;

import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;

public final class EventFactory {
  private EventFactory() {}

  public static MetricEvent event(String name, int pairs) {
    MetricEvent.Builder b = MetricEvent.newBuilder().setName(name);
    for (int i = 0; i < pairs; i++) {
      b.addTs(i + 1);
      b.addVals((float) (i + 1));
    }
    return b.build();
  }

  public static WalRecord batch(int count, int pairsPerEvent) {
    MetricEventBatch.Builder mb = MetricEventBatch.newBuilder();
    for (int i = 0; i < count; i++) {
      mb.addEvents(event("e" + i, pairsPerEvent));
    }
    return WalRecord.newBuilder().setEvent(mb.build()).build();
  }
}

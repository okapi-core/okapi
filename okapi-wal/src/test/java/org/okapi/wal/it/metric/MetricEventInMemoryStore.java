package org.okapi.wal.it.metric;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.WalStreamConsumer;

/**
 * Lightweight in-memory store for MetricEvent batches. - Tracks total events processed and
 * lastAppliedLsn. - Provides snapshot save/load (plain text) for tests/integration. - Implements
 * WalStreamConsumer idempotently.
 */
public final class MetricEventInMemoryStore implements WalStreamConsumer {

  private final AtomicLong lastAppliedLsn = new AtomicLong(-1);
  private final AtomicLong totalEvents = new AtomicLong(0);

  @Override
  public void consume(long lsn, WalRecord record) {
    long curr = lastAppliedLsn.get();
    if (lsn <= curr) {
      return; // idempotent skip
    }
    int added = 0;
    if (record.hasEvent()) {
      MetricEventBatch b = record.getEvent();
      added = b.getEventsCount();
    }
    totalEvents.addAndGet(added);
    lastAppliedLsn.updateAndGet(prev -> Math.max(prev, lsn));
  }

  public long lastAppliedLsn() {
    return lastAppliedLsn.get();
  }

  public long totalEvents() {
    return totalEvents.get();
  }

  public void loadSnapshot(Path snapshotFile) throws IOException {
    if (!Files.exists(snapshotFile)) return;
    var lines = Files.readAllLines(snapshotFile, StandardCharsets.UTF_8);
    long lsn = -1;
    long count = 0;
    for (String ln : lines) {
      if (ln.startsWith("lsn=")) lsn = Long.parseLong(ln.substring(4).trim());
      else if (ln.startsWith("events=")) count = Long.parseLong(ln.substring(7).trim());
    }
    lastAppliedLsn.set(lsn);
    totalEvents.set(count);
  }

  public void saveSnapshot(Path snapshotFile) throws IOException {
    String s = "lsn=" + lastAppliedLsn.get() + "\n" + "events=" + totalEvents.get() + "\n";
    Files.writeString(
        snapshotFile,
        s,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
  }
}

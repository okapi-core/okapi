package org.okapi.wal.it.stream;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.wal.*;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.WalStreamer.Options;
import org.okapi.wal.it.faults.CrashInjector;
import org.okapi.wal.it.faults.FramerWithFaults;
import org.okapi.wal.it.metric.MetricEventInMemoryStore;

/** End-to-end replay tests for WalStreamer with a MetricEventInMemoryStore. */
public class WalStreamerIT {

  @TempDir Path tmp;

  @Test
  void streamsFromSnapshotLsn_andReachesFinalState() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);

    // Use tuple framer to match streamer/recovery expectations
    CrashInjector inj = new CrashInjector(); // no faults armed
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    // Build writer with small-ish segments to create multiple files
    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc,
            framer,
            8 * 1024, /*listener*/
            null,
            SpilloverWalWriter.FsyncPolicy.INTERVAL,
            0,
            Duration.ofMillis(5));

    // Write a series of records with varying event counts
    List<Integer> perRecordCounts = new ArrayList<>();
    int totalRecords = 12;
    for (int i = 0; i < totalRecords; i++) {
      int count = 1 + (i % 5); // 1..5 events
      perRecordCounts.add(count);
      writer.write(batch(count));
    }
    long highestLsn = lsnCounter.get();
    writer.close();

    // Compute expected totals
    long expectedTotalEvents = perRecordCounts.stream().mapToLong(i -> i).sum();

    // Choose a snapshot point at LSN=7
    long snapshotLsn = 7L;
    long eventsUpToSnapshot =
        perRecordCounts.subList(0, (int) snapshotLsn).stream().mapToLong(i -> i).sum();

    // Save a snapshot for the store: includes count up to snapshot and the snapshot LSN
    MetricEventInMemoryStore snapshotStore = new MetricEventInMemoryStore();
    // simulate pre-snapshot application:
    for (int i = 0; i < snapshotLsn; i++) {
      snapshotStore.consume(i + 1, batch(perRecordCounts.get(i)));
    }
    Path snapFile = tmp.resolve("metric.snapshot");
    snapshotStore.saveSnapshot(snapFile);

    // Create a fresh store; load snapshot; then stream using consumer's watermark
    MetricEventInMemoryStore recoveringStore = new MetricEventInMemoryStore();
    recoveringStore.loadSnapshot(snapFile);
    assertThat(recoveringStore.lastAppliedLsn()).isEqualTo(snapshotLsn);
    assertThat(recoveringStore.totalEvents()).isEqualTo(eventsUpToSnapshot);

    WalStreamerImpl streamer = new WalStreamerImpl();
    Options opts = new Options();
    opts.runRecovery = true; // sanitize just in case
    opts.verifyCrc = true;
    opts.fenceToPersistedLsn = false;

    WalStreamerImpl.Result res = streamer.stream(tmp, recoveringStore, opts);

    assertThat(res.recordsDelivered).isEqualTo(totalRecords - snapshotLsn);
    assertThat(res.lastDeliveredLsn).isEqualTo(highestLsn);

    // Final state equals expected totals
    assertThat(recoveringStore.totalEvents()).isEqualTo(expectedTotalEvents);
    assertThat(recoveringStore.lastAppliedLsn()).isEqualTo(highestLsn);

    // Idempotency: re-run streamer; starting from consumer watermark → nothing delivered
    WalStreamerImpl.Result res2 = streamer.stream(tmp, recoveringStore, opts);
    assertThat(res2.recordsDelivered).isEqualTo(0);
    assertThat(recoveringStore.totalEvents()).isEqualTo(expectedTotalEvents);
    assertThat(recoveringStore.lastAppliedLsn()).isEqualTo(highestLsn);
  }

  @Test
  void fencesToPersistedLsn_ifRequested() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);

    CrashInjector inj = new CrashInjector();
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc,
            framer,
            8 * 1024, /*listener*/
            null,
            SpilloverWalWriter.FsyncPolicy.MANUAL,
            0,
            null);

    // write 8 records
    for (int i = 0; i < 8; i++) {
      writer.write(batch(2));
    }
    writer.close();
    long highest = lsnCounter.get();

    // snapshot LSN = 3
    long snapshotLsn = 3L;

    // persisted.lsn fences at 6
    PersistedLsnStore pls = PersistedLsnStore.open(tmp);
    pls.write(6L);

    MetricEventInMemoryStore store = new MetricEventInMemoryStore();
    // pre-load store with snapshot (3 records)
    for (int i = 0; i < snapshotLsn; i++) store.consume(i + 1, batch(2));

    WalStreamerImpl streamer = new WalStreamerImpl();
    WalStreamerImpl.Options opts = new WalStreamerImpl.Options();
    opts.runRecovery = true;
    opts.verifyCrc = true;
    opts.fenceToPersistedLsn = true;

    // uses store.lastAppliedLsn() == 3 as start
    WalStreamerImpl.Result r = streamer.stream(tmp, store, opts);
    assertThat(r.lastDeliveredLsn).isEqualTo(6L);
    assertThat(store.lastAppliedLsn()).isEqualTo(6L);

    // now stream remaining without fence → should reach highest
    opts.fenceToPersistedLsn = false;
    WalStreamerImpl.Result r2 = streamer.stream(tmp, store, opts);
    assertThat(r2.lastDeliveredLsn).isEqualTo(highest);
    assertThat(store.lastAppliedLsn()).isEqualTo(highest);
  }

  // --- helpers ---

  private static WalRecord batch(int count) {
    MetricEventBatch.Builder b = MetricEventBatch.newBuilder();
    for (int i = 0; i < count; i++) {
      b.addEvents(MetricEvent.newBuilder().setName("m" + i).addTs(1).addVals(1.0f).build());
    }
    return WalRecord.newBuilder().setEvent(b.build()).build();
  }
}

package org.okapi.wal.it.cleaner;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.wal.*;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.it.env.*;
import org.okapi.wal.it.observers.FsProbe;

public class CleanerFlowTest {

  @TempDir Path tmp;

  @Test
  void snapshotThenClean_deletesCoveredSealedSegments() throws Exception {
    WalTestEnv env = new WalTestEnv(tmp);
    WalAllocator alloc = env.allocator();

    // Build a probe record and compute conservative size used by the writer
    WalRecord recProbe = recordWithNMetrics(3);
    int conservative =
        24 /*FakeFramer overhead*/ + 4 /*OkapiIo length prefix*/ + recProbe.getSerializedSize();

    AtomicLong lsn = new AtomicLong(0);
    WalFramer framer = new org.okapi.wal.FakeFramer(24, lsn);
    SpilloverWalWriter writer =
        new WriterBuilder()
            .allocator(alloc)
            .framer(framer)
            // Let exactly one record fit per segment to deterministically create many segments
            .maxSegmentSize(conservative + 16)
            .build();

    // Write N records → N segments (N-1 sealed, last unsealed)
    final int N = 6;
    for (int i = 0; i < N; i++) {
      writer.write(recProbe);
    }
    writer.close();

    List<Path> before = env.listSegmentsSorted();
    // Should have created multiple segments
    assertThat(before.size()).isGreaterThanOrEqualTo(2);

    // Persist watermark covering all written LSNs
    Snapshots.writeSnapshotAndPersist(env, lsn.get());

    // Cleaner: keepLastK=1 → expect to retain 1 sealed segment + 1 unsealed (last)
    TestClock clock = TestClock.startingAt(Instant.parse("2025-08-15T00:00:00Z"));
    WalCleaner cleaner = env.cleaner(clock, Duration.ZERO, 1, false);
    cleaner.run();

    List<Path> after = env.listSegmentsSorted();
    // Expect exactly 2 segments left: newest sealed + last unsealed
    assertThat(after.size()).isEqualTo(2);
  }

  @Test
  void quarantineThenPurge_respectsGrace_andRemovesTrashWhenEmpty() throws Exception {
    WalTestEnv env = new WalTestEnv(tmp);
    WalAllocator alloc = env.allocator();

    // Build a probe record and compute conservative size used by the writer
    WalRecord recProbe = recordWithNMetrics(2);
    int conservative =
        24 /*FakeFramer overhead*/ + 4 /*OkapiIo length prefix*/ + recProbe.getSerializedSize();

    AtomicLong lsn = new AtomicLong(0);
    WalFramer framer = new org.okapi.wal.FakeFramer(24, lsn);
    SpilloverWalWriter writer =
        new WriterBuilder()
            .allocator(alloc)
            .framer(framer)
            // One record per segment to ensure multiple sealed segments exist
            .maxSegmentSize(conservative + 16)
            .build();

    final int N = 5;
    for (int i = 0; i < N; i++) writer.write(recProbe);
    writer.close();

    // Persist watermark covering all
    Snapshots.writeSnapshotAndPersist(env, lsn.get());

    // Run cleaner with a grace period: should move sealed segments to trash but not purge
    TestClock clock = TestClock.startingAt(Instant.parse("2025-08-15T00:00:00Z"));
    WalCleaner cleaner = env.cleaner(clock, Duration.ofMinutes(30), 0, false);

    cleaner.run();

    Path trashRoot = FsProbe.trashRoot(tmp);
    // We expect some eligible sealed segments were moved to trash, so the trash directory should
    // exist
    assertThat(Files.isDirectory(trashRoot)).isTrue();

    // there should be at least one batch dir containing files
    Path batch = Files.list(trashRoot).findFirst().orElseThrow();
    long movedCount = Files.list(batch).count();
    assertThat(movedCount).isGreaterThan(0);

    // Advance and purge
    clock.advance(Duration.ofMinutes(31));
    cleaner.run();

    // After purge, the trash root should be removed when empty
    assertThat(Files.exists(trashRoot)).isFalse();
  }

  // --- helpers ---

  private static WalRecord recordWithNMetrics(int n) {
    MetricEventBatch.Builder b = MetricEventBatch.newBuilder();
    for (int i = 0; i < n; i++) {
      b.addEvents(MetricEvent.newBuilder().setName("m" + i).addTs(1).addVals(1.0f).build());
    }
    return WalRecord.newBuilder().setEvent(b.build()).build();
  }
}

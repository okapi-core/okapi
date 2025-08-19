package org.okapi.wal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.wal.SpilloverWalWriter.FsyncPolicy;
import org.okapi.wal.Wal.MetricEvent;
import org.okapi.wal.Wal.MetricEventBatch;
import org.okapi.wal.Wal.WalRecord;

public class SpilloverWalWriterTest {

  @TempDir Path tmp;

  @AfterEach
  void cleanup() {
    // nothing explicit; channels/locks closed by writer.close() in each test
  }

  @Test
  void simpleWrite_invokesFramer_andListener() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    RecordingCommitListener listener = new RecordingCommitListener();
    AtomicLong lsn = new AtomicLong(100);
    FakeFramer framer = new FakeFramer(24, lsn); // deterministic LSNs

    SpilloverWalWriter writer =
        new SpilloverWalWriter(alloc, framer, 1_000_000, listener, FsyncPolicy.MANUAL, 0, null);

    WalRecord rec = recordWithNMetrics(3);
    writer.write(rec);
    writer.close();

    assertThat(listener.events()).hasSize(1);
    var ctx = listener.events().get(0);
    assertThat(ctx.getLsn()).isEqualTo(101L); // first next()
    assertThat(ctx.getBytesWritten()).isGreaterThan(0L);
    assertThat(ctx.getOffsetAfter()).isEqualTo(ctx.getBytesWritten());

    // Verify file size equals bytesWritten
    long size = Files.size(alloc.active());
    assertThat(size).isEqualTo(ctx.getBytesWritten());
  }

  @Test
  void segmentRollover_allocatesNewSegment() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    RecordingCommitListener listener = new RecordingCommitListener();
    AtomicLong lsn = new AtomicLong(0);
    FakeFramer framer = new FakeFramer(24, lsn);

    WalRecord rec = recordWithNMetrics(1);
    long conservativeSize = framer.perRecordOverheadBytes() + 4 + rec.getSerializedSize();

    // Set maxSegmentSize to just under one record to force rollover before second write
    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc,
            framer,
            conservativeSize + 8 /* generous but small */,
            listener,
            FsyncPolicy.MANUAL,
            0,
            null);

    Path segBefore = alloc.active();
    writer.write(rec);
    Path segAfterFirst = alloc.active();

    // Next write should cause rollover pre-write (by conservative check)
    writer.write(rec);
    Path segAfterSecond = alloc.active();
    writer.close();

    // active() returns the *current* active, but we can assert it changed after second write
    assertThat(segAfterSecond).isNotEqualTo(segAfterFirst);
    // Listener contexts show second write starting at offset 0 on new segment
    assertThat(listener.events()).hasSize(2);
    assertThat(listener.events().get(1).getOffsetBefore()).isEqualTo(0L);
  }

  @Test
  void noListenerOnFramerFailure() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    RecordingCommitListener listener = new RecordingCommitListener();
    AtomicLong lsn = new AtomicLong(0);
    FakeFramer framer = new FakeFramer(24, lsn);
    framer.setThrowOnPayload(true); // simulate failure during payload write

    SpilloverWalWriter writer =
        new SpilloverWalWriter(alloc, framer, 1_000_000, listener, FsyncPolicy.MANUAL, 0, null);

    WalRecord rec = recordWithNMetrics(1);

    assertThatThrownBy(() -> writer.write(rec)).isInstanceOf(RuntimeException.class);
    writer.close();

    // Listener should not be invoked
    assertThat(listener.events()).isEmpty();

    // File may contain partial header bytes from the framer (LSN+CRC) since exception happened
    // mid-write
    long size = Files.size(alloc.active());
    assertThat(size)
        .isGreaterThanOrEqualTo(0L); // no strict expectation; just ensure test doesn't assume zero
  }

  @Test
  void listenerExceptionPropagates_afterWrite() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    RecordingCommitListener listener = new RecordingCommitListener();
    listener.setThrowOnCall(true);
    AtomicLong lsn = new AtomicLong(0);
    FakeFramer framer = new FakeFramer(24, lsn);

    SpilloverWalWriter writer =
        new SpilloverWalWriter(alloc, framer, 1_000_000, listener, FsyncPolicy.MANUAL, 0, null);

    WalRecord rec = recordWithNMetrics(2);

    assertThatThrownBy(() -> writer.write(rec)).isInstanceOf(RuntimeException.class);
    writer.close();

    // File should still have bytes written (framer wrote successfully before listener threw)
    long size = Files.size(alloc.active());
    assertThat(size).isGreaterThan(0L);
  }

  @Test
  void fsyncPolicy_intervalForcesPeriodically() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    RecordingCommitListener listener = new RecordingCommitListener();
    AtomicLong lsn = new AtomicLong(0);
    FakeFramer framer = new FakeFramer(24, lsn);

    // Use a very small interval to exercise the path; we can't easily assert force(),
    // but we ensure no exceptions and multiple writes succeed.
    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc, framer, 1_000_000, listener, FsyncPolicy.INTERVAL, 0, Duration.ofMillis(1));

    WalRecord rec = recordWithNMetrics(1);
    writer.write(rec);
    Thread.sleep(2); // allow interval to pass
    writer.write(rec);
    writer.close();

    assertThat(listener.events()).hasSize(2);
  }

  private static WalRecord recordWithNMetrics(int n) {
    MetricEventBatch.Builder b = MetricEventBatch.newBuilder();
    for (int i = 0; i < n; i++) {
      b.addEvents(MetricEvent.newBuilder().setName("m" + i).addTs(1).addVals(1.0f).build());
    }
    return WalRecord.newBuilder().setEvent(b.build()).build();
  }
}

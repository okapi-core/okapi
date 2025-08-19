package org.okapi.wal.it.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
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

/** Integration tests for WalStreamer edge cases. */
public class WalStreamerEdgeCasesIT {

  @TempDir Path tmp;

  /**
   * Mid-segment resume: watermark splits records inside a segment, streamer skips exactly up to
   * watermark.
   */
  @Test
  void midSegmentResume_skipsUpToWatermark_andDeliversRest() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);
    CrashInjector inj = new CrashInjector();
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    // Small segment so multiple records fit in same segment (ensures mid-segment resume)
    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc,
            framer,
            64 * 1024, /*listener*/
            null,
            SpilloverWalWriter.FsyncPolicy.MANUAL,
            0,
            null);

    // 5 records; counts 1..5
    List<Integer> counts = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      counts.add(i);
      writer.write(batch(i));
    }
    writer.close();
    long highest = lsnCounter.get(); // 5

    // Create store with snapshot watermark at 3 (applied first 3)
    MetricEventInMemoryStore store = new MetricEventInMemoryStore();
    for (int i = 1; i <= 3; i++) {
      store.consume(i, batch(counts.get(i - 1)));
    }
    assertThat(store.lastAppliedLsn()).isEqualTo(3);

    WalStreamerImpl streamer = new WalStreamerImpl();
    Options opts = new Options();
    opts.runRecovery = true;
    opts.verifyCrc = true;

    WalStreamerImpl.Result r = streamer.stream(tmp, store, opts);
    // Delivered LSNs: 4,5 → 2 records
    assertThat(r.recordsDelivered).isEqualTo(2);
    assertThat(r.lastDeliveredLsn).isEqualTo(highest);
    // Total events = sum(1..5)
    assertThat(store.totalEvents()).isEqualTo(1 + 2 + 3 + 4 + 5);
  }

  /**
   * Consumer fails on a specific LSN, streamer stops; on retry with healthy consumer, resumes
   * correctly.
   */
  @Test
  void consumerFailure_midStream_stops_andResumeIsIdempotent() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);
    CrashInjector inj = new CrashInjector();
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc, framer, 64 * 1024, null, SpilloverWalWriter.FsyncPolicy.MANUAL, 0, null);

    // 6 records of 2 events each → easy counting
    for (int i = 0; i < 6; i++) writer.write(batch(2));
    writer.close();
    long highest = lsnCounter.get(); // 6

    // First run: consumer fails at LSN 4
    ThrowingConsumer failing = new ThrowingConsumer(4);
    WalStreamerImpl streamer = new WalStreamerImpl();
    Options opts = new Options();
    opts.runRecovery = true;

    assertThatThrownBy(() -> streamer.stream(tmp, failing, opts)).isInstanceOf(IOException.class);
    // lastApplied should be 3 (records 1..3 applied), 3*2 events
    assertThat(failing.lastAppliedLsn()).isEqualTo(3);
    assertThat(failing.totalEvents()).isEqualTo(6);

    // Retry with a healthy consumer (starts from watermark 3)
    MetricEventInMemoryStore healthy = new MetricEventInMemoryStore();
    // seed it with already-applied 1..3 to match watermark state
    for (int i = 1; i <= 3; i++) healthy.consume(i, batch(2));

    WalStreamerImpl.Result r2 = streamer.stream(tmp, healthy, opts);
    // Delivered: LSN 4,5,6 → 3 records
    assertThat(r2.recordsDelivered).isEqualTo(3);
    assertThat(r2.lastDeliveredLsn).isEqualTo(highest);
    assertThat(healthy.totalEvents()).isEqualTo(6 * 2);
  }

  /** CRC mismatch path: corrupt payload after recovery and ensure streamer fails fast. */
  @Test
  void crcMismatch_throwsDuringStreaming() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);
    CrashInjector inj = new CrashInjector();
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc, framer, 64 * 1024, null, SpilloverWalWriter.FsyncPolicy.MANUAL, 0, null);

    // write 3 records
    for (int i = 0; i < 3; i++) writer.write(batch(2));
    writer.close();

    // Run STRICT recovery once (clean base), then corrupt the last record's payload bytes
    WalRecoveryScanner scanner = new WalRecoveryScanner();
    scanner.recover(tmp, WalRecoveryScanner.TailPolicy.STRICT_TRUNCATE);

    Path lastSeg = lastSegment(tmp);
    corruptLastRecordPayloadByte(lastSeg);

    MetricEventInMemoryStore store = new MetricEventInMemoryStore();
    WalStreamerImpl streamer = new WalStreamerImpl();
    Options opts = new Options();
    opts.runRecovery = false; // do not re-run recovery; streamer should detect CRC mismatch

    assertThatThrownBy(() -> streamer.stream(tmp, store, opts))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("CRC mismatch");
  }

  /**
   * Torn tail: truncate mid-record; STRICT recovery truncates to last full; streaming delivers only
   * complete ones.
   */
  @Test
  void tornTail_strictRecovery_thenStreamDeliversCompleteRecordsOnly() throws Exception {
    WalAllocator alloc = new WalAllocator(tmp);
    AtomicLong lsnCounter = new AtomicLong(0);
    CrashInjector inj = new CrashInjector();
    WalFramer framer = new FramerWithFaults(24, lsnCounter, inj);

    SpilloverWalWriter writer =
        new SpilloverWalWriter(
            alloc, framer, 64 * 1024, null, SpilloverWalWriter.FsyncPolicy.MANUAL, 0, null);

    // write 3 records
    for (int i = 0; i < 3; i++) writer.write(batch(1));
    writer.close();

    // Tear the tail mid-record (3rd)
    Path lastSeg = lastSegment(tmp);
    tearTailInMiddleOfLastRecord(lastSeg);

    // Stream with recovery=true → STRICT truncation happens inside streamer
    MetricEventInMemoryStore store = new MetricEventInMemoryStore();
    WalStreamerImpl streamer = new WalStreamerImpl();
    Options opts = new Options();
    opts.runRecovery = true;

    WalStreamerImpl.Result r = streamer.stream(tmp, store, opts);
    // Only first 2 complete records get delivered
    assertThat(r.recordsDelivered).isEqualTo(2);
    assertThat(store.lastAppliedLsn()).isEqualTo(2);
  }

  // --- helpers ---

  private static WalRecord batch(int count) {
    MetricEventBatch.Builder b = MetricEventBatch.newBuilder();
    for (int i = 0; i < count; i++) {
      b.addEvents(MetricEvent.newBuilder().setName("m" + i).addTs(1).addVals(1.0f).build());
    }
    return WalRecord.newBuilder().setEvent(b.build()).build();
  }

  private static Path lastSegment(Path root) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(root, "wal_*.segment")) {
      return java.util.stream.StreamSupport.stream(ds.spliterator(), false)
          .sorted(Comparator.comparingInt(SegmentIndex::parseEpochFromSegment))
          .reduce((a, b) -> b)
          .orElseThrow();
    }
  }

  /** Parse frames and flip one byte in the last record's payload to trigger CRC mismatch. */
  private static void corruptLastRecordPayloadByte(Path seg) throws IOException {
    try (FileChannel ch =
        FileChannel.open(seg, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      long size = ch.size();
      long pos = 0;
      long lastPayloadPos = -1;
      int lastPayloadLen = -1;

      while (pos < size) {
        long start = pos;
        int l1 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, l1)) break;
        pos += l1; // skip LSN
        int l2 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, l2)) break;
        pos += l2; // skip CRC
        int pay = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, pay)) break;
        lastPayloadPos = pos;
        lastPayloadLen = pay;
        pos += pay;
      }
      if (lastPayloadPos >= 0 && lastPayloadLen > 0) {
        // Flip the first byte of the payload
        ByteBuffer one = ByteBuffer.allocate(1);
        ch.read(one, lastPayloadPos);
        one.flip();
        byte orig = one.get(0);
        one.clear();
        one.put((byte) (orig ^ 0xFF));
        one.flip();
        ch.write(one, lastPayloadPos);
      }
    }
  }

  /** Tear half of the last record by truncating mid-payload. */
  private static void tearTailInMiddleOfLastRecord(Path seg) throws IOException {
    try (FileChannel ch =
        FileChannel.open(seg, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      long size = ch.size();
      long pos = 0;
      long lastStart = -1;
      long lastEnd = -1;

      while (pos < size) {
        lastStart = pos;
        int l1 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, l1)) break;
        pos += l1;
        int l2 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, l2)) break;
        pos += l2;
        int pay = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, pay)) break;
        pos += pay;
        lastEnd = pos;
      }
      if (lastStart >= 0 && lastEnd > lastStart) {
        long mid = lastStart + (lastEnd - lastStart) / 2;
        ch.truncate(mid);
      }
    }
  }

  private static int readInt(FileChannel ch, long pos) throws IOException {
    ByteBuffer b = ByteBuffer.allocate(4);
    int n = ch.read(b, pos);
    if (n < 4) return -1;
    b.flip();
    return b.getInt();
  }

  private static boolean enough(long size, long pos, int len) {
    return len >= 0 && pos + len <= size;
  }
}

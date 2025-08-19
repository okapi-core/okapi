package org.okapi.wal;

import com.google.protobuf.InvalidProtocolBufferException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.CRC32C;
import org.okapi.wal.Wal.Crc32;
import org.okapi.wal.Wal.Lsn;
import org.okapi.wal.Wal.WalRecord;

/**
 * Streams WAL records strictly AFTER a given LSN, in order, to a WalStreamConsumer. Intended for
 * startup/recovery; not optimized for low latency.
 */
public final class WalStreamerImpl implements WalStreamer {

  private static final Pattern SEGMENT_PATTERN = Pattern.compile("^wal_\\d{10}\\.segment$");

  /** Stream all records with LSN > startAfterLsn to consumer, honoring {@link Options}. */
  public Result streamFrom(Path root, long startAfterLsn, WalStreamConsumer consumer, Options opts)
      throws IOException {
    if (opts == null) opts = new Options();

    // 1) Optional recovery
    if (opts.runRecovery) {
      WalRecoveryScanner scanner = new WalRecoveryScanner();
      scanner.recover(root, WalRecoveryScanner.TailPolicy.STRICT_TRUNCATE);
    }

    // 2) Determine upper LSN bound if requested
    Long upperLsn = null;
    if (opts.upToLsn != null) {
      upperLsn = opts.upToLsn;
    } else if (opts.fenceToPersistedLsn) {
      PersistedLsnStore store = PersistedLsnStore.open(root);
      long persisted = store.read();
      if (persisted >= 0) upperLsn = persisted;
    }

    // 3) Read fence: list segments & capture sizes
    List<SegmentView> views = snapshotSegments(root);

    long applied = 0;
    long lastLsn = startAfterLsn;
    int visited = 0;

    // 4) Stream
    for (SegmentView v : views) {
      // quick skip using sidecar: whole segment at/below startAfterLsn
      if (v.maxLsn != Long.MIN_VALUE && v.maxLsn <= startAfterLsn) continue;

      visited++;
      try (FileChannel ch = FileChannel.open(v.path, StandardOpenOption.READ)) {
        long pos = 0;
        final long fenceSize = v.sizeFence;

        while (pos < fenceSize) {
          long recordStart = pos;

          // [len][LSN]
          int len1 = readInt(ch, pos);
          pos += 4;
          if (!enough(fenceSize, pos, len1))
            break; // torn at fence; recovery should avoid this, but stop safely
          byte[] lsnBytes = readBytes(ch, pos, len1);
          pos += len1;
          long lsnVal = parseLsn(lsnBytes);

          // [len][CRC]
          int len2 = readInt(ch, pos);
          pos += 4;
          if (!enough(fenceSize, pos, len2)) break;
          byte[] crcBytes = readBytes(ch, pos, len2);
          pos += len2;
          long crcVal = parseCrc(crcBytes);

          // [len][payload]
          int payLen = readInt(ch, pos);
          pos += 4;
          if (!enough(fenceSize, pos, payLen)) break;
          byte[] payload = readBytes(ch, pos, payLen);
          pos += payLen;

          if (opts.verifyCrc) {
            CRC32C crc32c = new CRC32C();
            crc32c.update(payload, 0, payload.length);
            long computed = Integer.toUnsignedLong((int) crc32c.getValue());
            if (computed != crcVal) {
              throw new IOException("CRC mismatch at " + v.path + " offset " + recordStart);
            }
          }

          // honor lower bound
          if (lsnVal <= startAfterLsn) {
            continue;
          }
          // honor optional upper bound
          if (upperLsn != null && lsnVal > upperLsn) {
            return new Result(applied, lastLsn, visited);
          }

          WalRecord rec;
          try {
            rec = WalRecord.parseFrom(payload);
          } catch (InvalidProtocolBufferException e) {
            throw new IOException("Invalid WalRecord at " + v.path + " offset " + recordStart, e);
          }

          try {
            consumer.consume(lsnVal, rec);
          } catch (Exception ex) {
            throw new IOException("Consumer failed at LSN " + lsnVal, ex);
          }

          applied++;
          lastLsn = lsnVal;
        }
      }
    }

    return new Result(applied, lastLsn, visited);
  }

  // --- helpers ---

  private static List<SegmentView> snapshotSegments(Path root) throws IOException {
    List<SegmentView> out = new ArrayList<>();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
      for (Path p : ds) {
        String name = p.getFileName().toString();
        if (!SEGMENT_PATTERN.matcher(name).matches()) continue;
        long size = Files.size(p);
        SegmentIndex idx = SegmentIndex.loadOrNew(p);
        out.add(new SegmentView(p, size, idx.maxLsn));
      }
    }
    out.sort(Comparator.comparingInt(sv -> SegmentIndex.parseEpochFromSegment(sv.path)));
    return out;
  }

  private static boolean enough(long sizeFence, long pos, int len) {
    return len >= 0 && pos + len <= sizeFence;
  }

  private static int readInt(FileChannel ch, long pos) throws IOException {
    ByteBuffer b = ByteBuffer.allocate(4);
    int n = ch.read(b, pos);
    if (n < 4) return -1;
    b.flip();
    return b.getInt();
  }

  private static byte[] readBytes(FileChannel ch, long pos, int len) throws IOException {
    ByteBuffer b = ByteBuffer.allocate(len);
    int read = 0;
    while (b.hasRemaining()) {
      int n = ch.read(b, pos + read);
      if (n < 0) break;
      read += n;
    }
    if (read != len) return new byte[0];
    return b.array();
  }

  private static long parseLsn(byte[] bytes) throws InvalidProtocolBufferException {
    return Lsn.parseFrom(bytes).getN();
  }

  private static long parseCrc(byte[] bytes) throws InvalidProtocolBufferException {
    return Crc32.parseFrom(bytes).getCrc();
  }

  private static final class SegmentView {
    final Path path;
    final long sizeFence; // size captured at fence time
    final long maxLsn;

    SegmentView(Path path, long sizeFence, long maxLsn) {
      this.path = path;
      this.sizeFence = sizeFence;
      this.maxLsn = maxLsn;
    }
  }

  @Override
  public Result stream(Path root, WalStreamConsumer consumer, Options opts) throws IOException {
    long startAfter = consumer.lastAppliedLsn();
    Result r = streamFrom(root, startAfter, consumer, opts);
    // Best-effort flush after streaming completes
    try {
      consumer.flush();
    } catch (Exception e) {
      throw new IOException("Consumer flush failed after streaming", e);
    }
    return r;
  }
}

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
 * Scans WAL segments to find the last valid record, optionally truncating a torn tail. Understands
 * "tuple" framing: [len][LSN], [len][CRC], [len][payload].
 */
public final class WalRecoveryScanner {

  public enum TailPolicy {
    STRICT_TRUNCATE,
    SALVAGE_CONTINUE
  }

  public static final class Result {
    public final long highestValidLsn;
    public final Path tailSegment;
    public final long truncateOffset; // -1 if no truncate
    public final List<Path> rebuiltIndexes;

    Result(long lsn, Path seg, long off, List<Path> rebuilt) {
      this.highestValidLsn = lsn;
      this.tailSegment = seg;
      this.truncateOffset = off;
      this.rebuiltIndexes = Collections.unmodifiableList(rebuilt);
    }
  }

  private static final Pattern SEGMENT_PATTERN = Pattern.compile("^wal_\\d{10}\\.segment$");

  public Result recover(Path root, TailPolicy policy) throws IOException {
    List<Path> segments = discover(root);
    long highestLsn = -1L;
    Path tailSeg = null;
    long truncOff = -1L;
    List<Path> rebuilt = new ArrayList<>();

    for (int i = 0; i < segments.size(); i++) {
      Path seg = segments.get(i);
      boolean isLast = (i == segments.size() - 1);
      ScanOutcome outcome = scanSegment(seg, policy, /*truncateAllowed*/ isLast);
      highestLsn = Math.max(highestLsn, outcome.maxLsn);
      if (outcome.truncated) {
        tailSeg = seg;
        truncOff = outcome.truncateOffset;
      }
      // rebuild/store sidecar
      SegmentIndex idx = new SegmentIndex(SegmentIndex.parseEpochFromSegment(seg));
      idx.minLsn = (outcome.minLsn == Long.MAX_VALUE) ? Long.MAX_VALUE : outcome.minLsn;
      idx.maxLsn = (outcome.maxLsn == Long.MIN_VALUE) ? Long.MIN_VALUE : outcome.maxLsn;
      idx.recordCount = outcome.count;
      idx.sealed = !isLast; // last is active
      idx.store(seg);
      rebuilt.add(SegmentIndex.indexPathFor(seg));
    }

    return new Result(highestLsn, tailSeg, truncOff, rebuilt);
  }

  private static List<Path> discover(Path root) throws IOException {
    List<Path> out = new ArrayList<>();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
      for (Path p : ds) {
        String name = p.getFileName().toString();
        if (SEGMENT_PATTERN.matcher(name).matches()) out.add(p);
      }
    }
    out.sort(Comparator.comparingInt(SegmentIndex::parseEpochFromSegment));
    return out;
  }

  private static final class ScanOutcome {
    long minLsn = Long.MAX_VALUE;
    long maxLsn = Long.MIN_VALUE;
    long count = 0;
    boolean truncated = false;
    long truncateOffset = -1L;
  }

  private static ScanOutcome scanSegment(Path seg, TailPolicy policy, boolean truncateAllowed)
      throws IOException {
    ScanOutcome out = new ScanOutcome();
    try (FileChannel ch =
        FileChannel.open(seg, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      long pos = 0;
      long size = ch.size();
      while (pos < size) {
        long recordStart = pos;

        // [len][LSN]
        int len1 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, len1)) return torn(policy, ch, truncateAllowed, recordStart, out);
        byte[] lsnBytes = readBytes(ch, pos, len1);
        pos += len1;
        long lsn = parseLsn(lsnBytes);

        // [len][CRC]
        int len2 = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, len2)) return torn(policy, ch, truncateAllowed, recordStart, out);
        byte[] crcBytes = readBytes(ch, pos, len2);
        pos += len2;
        long crcVal = parseCrc(crcBytes);

        // [len][payload]
        int payLen = readInt(ch, pos);
        pos += 4;
        if (!enough(size, pos, payLen)) return torn(policy, ch, truncateAllowed, recordStart, out);
        byte[] payload = readBytes(ch, pos, payLen);
        pos += payLen;

        // verify CRC32C(payload)
        CRC32C crc32c = new CRC32C();
        crc32c.update(payload, 0, payload.length);
        long computed = Integer.toUnsignedLong((int) crc32c.getValue());
        if (computed != crcVal) {
          // invalid record
          if (policy == TailPolicy.STRICT_TRUNCATE && truncateAllowed) {
            ch.truncate(recordStart);
            out.truncated = true;
            out.truncateOffset = recordStart;
          }
          break; // stop scanning this segment
        }

        // try parsing payload as WalRecord (basic sanity)
        try {
          WalRecord.parseFrom(payload);
        } catch (InvalidProtocolBufferException e) {
          if (policy == TailPolicy.STRICT_TRUNCATE && truncateAllowed) {
            ch.truncate(recordStart);
            out.truncated = true;
            out.truncateOffset = recordStart;
          }
          break;
        }

        // success
        out.count++;
        out.minLsn = Math.min(out.minLsn, lsn);
        out.maxLsn = Math.max(out.maxLsn, lsn);
      }
    }
    return out;
  }

  private static ScanOutcome torn(
      TailPolicy policy, FileChannel ch, boolean truncateAllowed, long recordStart, ScanOutcome out)
      throws IOException {
    if (policy == TailPolicy.STRICT_TRUNCATE && truncateAllowed) {
      ch.truncate(recordStart);
      out.truncated = true;
      out.truncateOffset = recordStart;
    }
    return out;
  }

  private static boolean enough(long size, long pos, int len) {
    return len >= 0 && pos + len <= size;
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
}

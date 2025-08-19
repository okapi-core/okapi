package org.okapi.wal;

import lombok.ToString;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

/**
 * Sidecar index for each WAL segment, stored as: wal_XXXXXXXXXX.segment.idx
 *
 * <p>Tracks min/max LSN, recordCount, and sealed flag. Writer updates this file on each commit and
 * at rollover/seal time.
 */
@ToString
public final class SegmentIndex {

  public static final String IDX_SUFFIX = ".idx";

  public final int epoch;
  public long minLsn = Long.MAX_VALUE;
  public long maxLsn = Long.MIN_VALUE;
  public long recordCount = 0;
  public boolean sealed = false;
  public long updatedAtMillis = 0;

  public SegmentIndex(int epoch) {
    this.epoch = epoch;
  }

  public static Path indexPathFor(Path segmentPath) {
    return segmentPath.resolveSibling(segmentPath.getFileName().toString() + IDX_SUFFIX);
  }

  public static int parseEpochFromSegment(Path segmentPath) {
    String name = segmentPath.getFileName().toString(); // wal_0000000001.segment
    int start = ("wal_").length();
    int end = name.length() - (".segment").length();
    String numeric = name.substring(start, end);
    return Integer.parseInt(numeric);
  }

  public static SegmentIndex loadOrNew(Path segmentPath) throws IOException {
    int epoch = parseEpochFromSegment(segmentPath);
    Path idxPath = indexPathFor(segmentPath);
    SegmentIndex idx = new SegmentIndex(epoch);
    if (Files.exists(idxPath)) {
      Properties p = new Properties();
      try (InputStream in = Files.newInputStream(idxPath)) {
        p.load(in);
      }
      idx.minLsn = Long.parseLong(p.getProperty("minLsn", String.valueOf(Long.MAX_VALUE)));
      idx.maxLsn = Long.parseLong(p.getProperty("maxLsn", String.valueOf(Long.MIN_VALUE)));
      idx.recordCount = Long.parseLong(p.getProperty("recordCount", "0"));
      idx.sealed = Boolean.parseBoolean(p.getProperty("sealed", "false"));
      idx.updatedAtMillis = Long.parseLong(p.getProperty("updatedAtMillis", "0"));
    }
    return idx;
  }

  public void store(Path segmentPath) throws IOException {
    Path idxPath = indexPathFor(segmentPath);
    Properties p = new Properties();
    p.setProperty("epoch", Integer.toString(epoch));
    p.setProperty("minLsn", Long.toString(minLsn));
    p.setProperty("maxLsn", Long.toString(maxLsn));
    p.setProperty("recordCount", Long.toString(recordCount));
    p.setProperty("sealed", Boolean.toString(sealed));
    updatedAtMillis = Instant.now().toEpochMilli();
    p.setProperty("updatedAtMillis", Long.toString(updatedAtMillis));

    // simple atomic-ish write: write temp then move
    Path tmp = idxPath.resolveSibling(idxPath.getFileName().toString() + ".tmp");
    try (OutputStream out = Files.newOutputStream(tmp)) {
      p.store(out, "WAL segment index");
    }
    Files.move(
        tmp,
        idxPath,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
  }

  @Override
  public int hashCode() {
    return Objects.hash(epoch, minLsn, maxLsn, recordCount, sealed, updatedAtMillis);
  }
}

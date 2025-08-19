package org.okapi.wal;

import lombok.ToString;

import java.nio.file.Path;

@ToString
/** Immutable context describing a single WAL record write. */
public final class WalCommitContext {
  private final long lsn;
  private final long crc32c;
  private final Path segmentPath;
  private final long offsetBefore; // file offset at which this write began
  private final long offsetAfter; // offset after this write (exclusive)
  private final long bytesWritten; // total bytes written by the framer for this record

  public WalCommitContext(
      long lsn,
      long crc32c,
      Path segmentPath,
      long offsetBefore,
      long offsetAfter,
      long bytesWritten) {
    this.lsn = lsn;
    this.crc32c = crc32c;
    this.segmentPath = segmentPath;
    this.offsetBefore = offsetBefore;
    this.offsetAfter = offsetAfter;
    this.bytesWritten = bytesWritten;
  }

  public long getLsn() {
    return lsn;
  }

  public long getCrc32c() {
    return crc32c;
  }

  public Path getSegmentPath() {
    return segmentPath;
  }

  public long getOffsetBefore() {
    return offsetBefore;
  }

  public long getOffsetAfter() {
    return offsetAfter;
  }

  public long getBytesWritten() {
    return bytesWritten;
  }
}

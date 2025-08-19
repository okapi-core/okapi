package org.okapi.wal;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.okapi.wal.Wal.WalRecord;

/**
 * Segmenting WAL writer using a WalFramer. Invokes a synchronous WalCommitListener after each
 * write.
 */
public class SpilloverWalWriter implements WalWriter<WalRecord> {

  private final WalAllocator allocator;
  private final WalFramer framer;
  private final long maxSegmentSize;
  private final WalCommitListener commitListener; // may be null

  private FileChannel activeSegment;
  private Path currentSegmentPath;
  private long writtenBytesInSegment = 0;
  private SegmentIndex currentIndex;

  private WalProcessLock writeLock;

  public enum FsyncPolicy {
    EVERY_RECORD,
    BYTES,
    INTERVAL,
    MANUAL
  }

  private final FsyncPolicy fsyncPolicy;
  private final long fsyncBytesThreshold; // for BYTES
  private final long fsyncIntervalNanos; // for INTERVAL
  private long bytesSinceLastForce = 0L;
  private long lastForceNanos = System.nanoTime();

  public SpilloverWalWriter(
      WalAllocator allocator,
      WalFramer framer,
      long maxSegmentSize,
      WalCommitListener commitListener,
      FsyncPolicy fsyncPolicy,
      long fsyncBytesThreshold,
      Duration fsyncInterval)
      throws IOException {
    this.allocator = allocator;
    this.framer = framer;
    this.maxSegmentSize = maxSegmentSize;
    this.commitListener = commitListener;
    this.fsyncPolicy = fsyncPolicy == null ? FsyncPolicy.MANUAL : fsyncPolicy;
    this.fsyncBytesThreshold = fsyncBytesThreshold;
    this.fsyncIntervalNanos = (fsyncInterval == null) ? 0L : fsyncInterval.toNanos();

    // ACQUIRE WRITE LOCK
    Map<String, String> info = new HashMap<>();
    info.put("role", "writer");
    info.put("thread", Thread.currentThread().getName());
    info.put("started_at", Long.toString(System.currentTimeMillis()));
    this.writeLock = WalProcessLock.acquire(allocator.active().getParent(), "write", info);

    this.currentSegmentPath = allocator.active();
    this.activeSegment =
        FileChannel.open(
            currentSegmentPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND);
    this.currentSegmentPath = allocator.active();
    this.currentIndex = SegmentIndex.loadOrNew(this.currentSegmentPath);
    this.currentIndex.sealed = false; // active segment is unsealed
    this.currentIndex.store(this.currentSegmentPath);
    this.writtenBytesInSegment = this.activeSegment.size();
  }

  @Override
  public synchronized void write(WalRecord record) throws IOException {
    long conservativeSize = framer.perRecordOverheadBytes() + 4L + record.getSerializedSize();
    if (writtenBytesInSegment + conservativeSize > maxSegmentSize) {
      spillOver();
    }

    long offsetBefore = writtenBytesInSegment;
    WalFramerResult res = framer.writeFramed(activeSegment, record);
    long offsetAfter = offsetBefore + res.bytesWritten;

    writtenBytesInSegment = offsetAfter;
    currentIndex.minLsn = Math.min(currentIndex.minLsn, res.lsn);
    currentIndex.maxLsn = Math.max(currentIndex.maxLsn, res.lsn);
    currentIndex.recordCount += 1;
    currentIndex.store(currentSegmentPath);
    // fsync policy
    bytesSinceLastForce += res.bytesWritten;
    maybeForce();

    if (commitListener != null) {
      // Synchronous callback; must be lightweight & idempotent by LSN.
      WalCommitContext ctx =
          new WalCommitContext(
              res.lsn, res.crc32c, currentSegmentPath, offsetBefore, offsetAfter, res.bytesWritten);
      commitListener.onWalCommit(ctx);
    }
  }

  // ADD this private helper in the class:
  private void maybeForce() throws IOException {
    switch (fsyncPolicy) {
      case EVERY_RECORD:
        activeSegment.force(true);
        bytesSinceLastForce = 0;
        lastForceNanos = System.nanoTime();
        break;
      case BYTES:
        if (bytesSinceLastForce >= fsyncBytesThreshold && fsyncBytesThreshold > 0) {
          activeSegment.force(true);
          bytesSinceLastForce = 0;
          lastForceNanos = System.nanoTime();
        }
        break;
      case INTERVAL:
        long now = System.nanoTime();
        if (fsyncIntervalNanos > 0 && (now - lastForceNanos) >= fsyncIntervalNanos) {
          activeSegment.force(true);
          bytesSinceLastForce = 0;
          lastForceNanos = now;
        }
        break;
      case MANUAL:
      default:
        // do nothing
    }
  }

  void spillOver() throws IOException {
    // Seal and persist the previous segment’s sidecar (if it exists)
    if (this.currentIndex != null) {
      this.currentIndex.sealed = true;
      this.currentIndex.store(this.currentSegmentPath);
    }

    // Allocate and open the next segment
    var next = allocator.allocate();
    FileChannel nextChannel =
        FileChannel.open(next, StandardOpenOption.APPEND, StandardOpenOption.CREATE);

    // Switch active channel/path
    if (this.activeSegment != null && this.activeSegment.isOpen()) {
      this.activeSegment.close();
    }
    this.activeSegment = nextChannel;
    this.currentSegmentPath = next;
    this.writtenBytesInSegment = this.activeSegment.size();

    // Initialize sidecar for the new active segment
    this.currentIndex = SegmentIndex.loadOrNew(this.currentSegmentPath);
    this.currentIndex.sealed = false;
    this.currentIndex.store(this.currentSegmentPath);
  }

  @Override
  public synchronized void close() throws IOException {
    if (activeSegment != null && activeSegment.isOpen()) {
      activeSegment.close();
    }
    if (writeLock != null) {
      try {
        writeLock.close();
      } catch (IOException ignored) {
      }
    }
  }
}

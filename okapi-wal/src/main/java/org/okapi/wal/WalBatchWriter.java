package org.okapi.wal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.okapi.wal.Wal.WalRecord;
import org.okapi.wal.exceptions.VeryHugeRecordException;

/**
 * Buffers events E and flushes them as WalRecords using a WalRecordAdapter and WalFramer-aware
 * sizing. Triggers: size (encoded bytes), count, or time.
 */
public class WalBatchWriter<E> implements AutoCloseable {

  private final WalRecordAdapter<E> adapter;
  private final WalWriter<WalRecord> writer;
  private final WalFramer framer;
  private final ScheduledExecutorService scheduler;
  private final long waitMillis;
  private final int maxBatchSize;
  private final int maxWalRecordSize;

  private final List<E> buffer = new ArrayList<>();

  private boolean flushRequested = false;
  private ScheduledFuture<?> pendingFlush = null;

  public WalBatchWriter(
      WalRecordAdapter<E> adapter,
      WalWriter<WalRecord> writer,
      WalFramer framer,
      ScheduledExecutorService scheduler,
      Duration waitBeforeBatchify,
      int maxBatchSize,
      int maxWalRecordSize) {

    this.adapter = checkNotNull(adapter, "adapter");
    this.writer = checkNotNull(writer, "writer");
    this.framer = checkNotNull(framer, "framer");
    this.scheduler = checkNotNull(scheduler, "scheduler");
    this.waitMillis = (waitBeforeBatchify == null) ? 0L : waitBeforeBatchify.getSeconds() * 1000L;
    checkArgument(maxBatchSize > 0, "maxBatchSize must be > 0");
    checkArgument(maxWalRecordSize > 8, "maxWalRecordSize must be reasonably large");
    this.maxBatchSize = maxBatchSize;
    this.maxWalRecordSize = maxWalRecordSize;
  }

  public synchronized void consume(E event, int serializedSize) throws IOException {
    if (event == null) return;
    adapter.validate(event);

    // Guard: single-item must fit (unchanged)
    WalRecord single = adapter.buildRecord(Collections.singletonList(event));
    int singleEncoded = framedEncodedSize(single);
    if (singleEncoded > maxWalRecordSize) {
      throw new VeryHugeRecordException();
    }

    // If adding this would exceed the item-count cap, flush what's already there first
    if (buffer.size() + 1 > maxBatchSize) {
      flushBufferedIntoWal();
    }

    // If adding this would exceed size cap, flush what's already there first
    if (!buffer.isEmpty()) {
      WalRecord candidate = adapter.buildRecord(peekWith(event));
      if (framedEncodedSize(candidate) > maxWalRecordSize) {
        flushBufferedIntoWal();
      }
    }

    // Enqueue
    buffer.add(event);

    // If we've exactly hit the count threshold, flush immediately
    if (buffer.size() >= maxBatchSize) {
      flushBufferedIntoWal();
    } else {
      // otherwise (we’re under the count cap), arm/reset the timer if configured
      ensureTimer();
    }
  }

  /** Runnable compatible with external scheduling; usually not needed since we auto-schedule. */
  public Runnable batchifyInterrupt() {
    return () -> {
      synchronized (WalBatchWriter.this) {
        flushRequested = true;
      }
    };
  }

  public synchronized void flushIfDue() throws IOException {
    if (flushRequested) {
      flushBufferedIntoWal();
    }
  }

  public synchronized void flush() throws IOException {
    flushBufferedIntoWal();
  }

  @Override
  public synchronized void close() throws IOException {
    cancelTimer();
    flushBufferedIntoWal();
  }

  // ---------------- internal (synchronized by callers) ----------------

  private List<E> peekWith(E next) {
    int n = buffer.size();
    List<E> view = new ArrayList<>(n + 1);
    view.addAll(buffer);
    view.add(next);
    return view;
  }

  private int framedEncodedSize(WalRecord record) {
    // Conservative bound: framer overhead (upper-bound) + length-prefix + proto payload
    return framer.perRecordOverheadBytes() + 4 + record.getSerializedSize();
  }

  private void ensureTimer() {
    if (waitMillis <= 0) return;
    if (pendingFlush != null && !pendingFlush.isDone()) {
      pendingFlush.cancel(false);
    }
    pendingFlush =
        scheduler.schedule(
            () -> {
              synchronized (WalBatchWriter.this) {
                flushRequested = true;
                try {
                  flushBufferedIntoWal();
                } catch (IOException e) {
                  throw new RuntimeException("Timed flush failed", e);
                }
              }
            },
            waitMillis,
            TimeUnit.MILLISECONDS);
  }

  private void cancelTimer() {
    if (pendingFlush != null) {
      pendingFlush.cancel(false);
      pendingFlush = null;
    }
  }

  private void flushBufferedIntoWal() throws IOException {
    if (buffer.isEmpty()) {
      flushRequested = false;
      cancelTimer();
      return;
    }

    int start = 0;
    final int n = buffer.size();

    while (start < n) {
      int endExclusive = Math.min(start + maxBatchSize, n);

      int lastGoodEnd = start - 1;
      WalRecord lastGood = null;

      for (int end = start + 1; end <= endExclusive; end++) {
        WalRecord candidate = adapter.buildRecord(buffer.subList(start, end));
        if (framedEncodedSize(candidate) <= maxWalRecordSize) {
          lastGoodEnd = end;
          lastGood = candidate;
        } else {
          break;
        }
      }

      if (lastGoodEnd < start) {
        // Should not happen (single-item pre-check), but guard anyway
        throw new VeryHugeRecordException();
      }

      writer.write(lastGood);
      start = lastGoodEnd;
    }

    buffer.clear();
    flushRequested = false;
    cancelTimer();
  }
}

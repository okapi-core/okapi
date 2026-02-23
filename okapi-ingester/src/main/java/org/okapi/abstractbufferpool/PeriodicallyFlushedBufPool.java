package org.okapi.abstractbufferpool;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.LogFileWriter;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.pages.BufferPool;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

@Slf4j
public abstract class PeriodicallyFlushedBufPool<
    BufferPoolInputType,
    P extends AppendOnlyPage<PageInputType, S, M, B>,
    PageInputType,
    S,
    M,
    B,
    Id> {
  private final LogFileWriter<P, S, M, B, Id> fileWriter;
  private final MeterRegistry meterRegistry;
  private final BufferPool<P, PageInputType, S, M, B, Id> pool;

  // counter names
  String pageFlushTotalCounterName;
  String pagesSerializedBytesCounterName;

  // input adapter: optionally transform incoming records to what the page itself accepts.
  // deduplicator -> getUniqueStreamKey()
  public PeriodicallyFlushedBufPool(
      BUFFER_POOL_TYPE poolType,
      Function<StreamIdentifier<Id>, P> pageFactory,
      Codec<P, S, M, B> codec,
      DiskLogBinPaths<Id> diskLogBinPaths,
      MeterRegistry meterRegistry,
      int sealedPageCap,
      long sealedPageTtlMs,
      WalResourcesPerStream<Id> walResourcesPerStream) {
    this.fileWriter = new LogFileWriter<>(codec, diskLogBinPaths, walResourcesPerStream);
    this.meterRegistry = meterRegistry;
    this.pageFlushTotalCounterName = "okapi." + poolType.name() + ".logs.page_flush_total";
    this.pagesSerializedBytesCounterName =
        "okapi." + poolType.name() + ".logs.pages_serialized_bytes";
    this.pool =
        new BufferPool<>(
            // page factory per (tenant, stream)
            pageFactory,
            // flusher
            (id, page) -> {
              try {
                var written = fileWriter.appendPage(id, page);
                this.meterRegistry.counter(pageFlushTotalCounterName).increment();
                this.meterRegistry.counter(pagesSerializedBytesCounterName).increment(written);
              } catch (Exception e) {
                log.error("Failed to persist page", e);
                throw e;
              }
            },
            sealedPageCap,
            sealedPageTtlMs);
  }

  public abstract void consume(
      Lsn lsn, StreamIdentifier<Id> identifier, BufferPoolInputType record);

  public S snapshotActivePage(StreamIdentifier<Id> streamIdentifier) {
    return pool.snapshotActive(streamIdentifier);
  }

  public List<S> snapshotSealedPages(StreamIdentifier<Id> streamIdentifier, long start, long end) {
    return pool.snapshotSealed(streamIdentifier, start, end);
  }

  public void flushPagesOlderThan(long boundaryTsMillis) {
    pool.flushPagesOlderThan(boundaryTsMillis);
  }

  public void flushAllNow() {
    pool.flushAllNow();
  }

  public void awaitFlushQueueEmpty(long timeoutMillis) {
    pool.awaitFlushQueueEmpty(timeoutMillis);
  }

  public void append(Lsn lsn, StreamIdentifier<Id> streamIdentifier, PageInputType record) {
    pool.append(lsn, streamIdentifier, record);
  }
}

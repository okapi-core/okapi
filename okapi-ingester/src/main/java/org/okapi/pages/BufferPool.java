package org.okapi.pages;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.okapi.retries.RetryingCallables;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

/** Generic buffer pool managing active and sealed pages, flushing, and TTL/cap eviction. */
@Slf4j
public final class BufferPool<P extends AppendOnlyPage<I, S, M, B>, I, S, M, B, Id>
    implements AutoCloseable {
  public static final long FLUSHER_MILLIS = 100L;
  public static final long SEALED_REAPER_MILLIS = 1000L; // 1 second
  private final Function<StreamIdentifier<Id>, P> pageFactory;
  private final int sealedCap;
  private final long sealedTtlMs;
  private final Map<Id, ActivePage<P, I, S, M, B, Id>> pages = new ConcurrentHashMap<>();
  private final Map<Id, Deque<SealedEntry<P>>> sealedPages = new ConcurrentHashMap<>();
  private final BlockingQueue<PendingFlush<P, Id>> flushQueue = new LinkedBlockingQueue<>();
  private final ScheduledExecutorService executor;

  public BufferPool(
      Function<StreamIdentifier<Id>, P> pageFactory,
      PageFlusher<P, Id> flusher,
      int sealedCap,
      long sealedTtlMs) {
    this.pageFactory = pageFactory;
    this.sealedCap = sealedCap;
    this.sealedTtlMs = sealedTtlMs;
    this.executor = Executors.newScheduledThreadPool(2);
    executor.scheduleWithFixedDelay(
        () -> {
          var batch = new ArrayList<PendingFlush<P, Id>>();
          flushQueue.drainTo(batch);
          for (var pf : batch) {
            try {
              flusher.flush(pf.getStreamIdentifier(), pf.getPage());
              markSealedPersisted(pf.getStreamIdentifier(), pf.getPage());
            } catch (Exception e) {
              log.error("Could not flush page stream {}", pf.getStreamIdentifier(), e);
            }
          }
        },
        0,
        FLUSHER_MILLIS,
        TimeUnit.MILLISECONDS);

    executor.scheduleWithFixedDelay(
        this::evictExpiredAndCap, 0, SEALED_REAPER_MILLIS, TimeUnit.MILLISECONDS);
  }

  @Override
  public void close() throws Exception {
    this.executor.close();
  }

  public void append(Lsn lsn, StreamIdentifier<Id> streamIdentifier, I record) {
    var ap =
        pages.computeIfAbsent(
            streamIdentifier.getStreamId(), k -> new ActivePage<>(streamIdentifier, pageFactory));
    Optional<P> sealed = ap.append(lsn, record);
    sealed.ifPresent(p -> sealPage(streamIdentifier, p));
  }

  public S snapshotActive(StreamIdentifier<Id> streamIdentifier) {
    var ap = pages.get(streamIdentifier.getStreamId());
    return ap == null ? null : ap.snapshot();
  }

  public List<S> snapshotSealed(StreamIdentifier<Id> streamIdentifier, long start, long end) {
    Deque<SealedEntry<P>> dq = sealedPages.get(streamIdentifier.getStreamId());
    if (dq == null || dq.isEmpty()) return List.of();
    var out = new ArrayList<S>();
    for (SealedEntry<P> se : dq) {
      var r = se.page.range();
      if (r.isEmpty()) continue;
      var rr = r.get();
      if (rr.endInclusive() < start || rr.startInclusive() > end) continue;
      out.add(se.page.snapshot());
    }
    return out;
  }

  public void flushPagesOlderThan(long boundaryTsMillis) {
    pages.forEach(
        (k, ap) -> {
          Optional<P> sealed = ap.rotateIfOlderThanAndNotEmpty(boundaryTsMillis);
          sealed.ifPresent(p -> sealPage(ap.getStreamIdentifier(), p));
        });
  }

  public void flushAllNow() {
    pages.forEach(
        (k, ap) -> {
          Optional<P> sealed = ap.rotateIfNonEmpty();
          sealed.ifPresent(p -> sealPage(ap.getStreamIdentifier(), p));
        });
  }

  public void awaitFlushQueueEmpty(long timeoutMillis) {
    int nAttempts = (int) (timeoutMillis / 50);
    RetryingCallables.retry(flushQueue::isEmpty, nAttempts, 50L);
  }

  void sealPage(StreamIdentifier<Id> identifier, P page) {
    if (page.isEmpty()) return;
    addSealed(identifier, page);
    flushQueue.offer(new PendingFlush<>(identifier, page));
  }

  private void addSealed(StreamIdentifier<Id> identifier, P page) {
    Deque<SealedEntry<P>> dq =
        sealedPages.computeIfAbsent(identifier.getStreamId(), kk -> new ArrayDeque<>());
    dq.addLast(new SealedEntry<>(page));
  }

  private void markSealedPersisted(StreamIdentifier<Id> streamIdentifier, P page) {
    Deque<SealedEntry<P>> dq = sealedPages.get(streamIdentifier.getStreamId());
    for (SealedEntry<P> se : dq) {
      if (se.page == page) {
        se.setPersistedAt(System.currentTimeMillis());
        break;
      }
    }
  }

  private void evictExpiredAndCap() {
    long now = System.currentTimeMillis();
    sealedPages.forEach(
        (k, dq) -> {
          while (!dq.isEmpty()) {
            var head = dq.peekFirst();
            if (head.persistedAt > 0 && now - head.persistedAt >= sealedTtlMs) {
              dq.removeFirst();
            } else {
              break;
            }
          }
          while (dq.size() > sealedCap) {
            dq.removeFirst();
          }
        });
  }
}

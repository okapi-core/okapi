package org.okapi.pages;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.wal.lsn.Lsn;

public class BufferPoolTests {

  Lsn mockLsn = Lsn.fromNumber(100);

  @Test
  void testAppend_FullPageIsRotatedOut() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 100), pageFlusher, 10, 100);
    // append content that will fill the page
    bp.append(
        mockLsn,
        new LogStreamIdentifier("stream1"),
        MockPageInput.builder().tsMillis(1000).content("log1").size(200).build());
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(() -> 1 == pageFlusher.getFlushedPages().size());
    var sealed = bp.snapshotSealed(new LogStreamIdentifier("stream1"), 0, Long.MAX_VALUE);
    assertEquals(1, sealed.size());
    bp.close();
  }

  @Test
  void testFlushOlderPages() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 1024), pageFlusher, 10, 100);
    // append content that will fill the page
    var sid = new LogStreamIdentifier("stream1");
    bp.append(
        mockLsn, sid, MockPageInput.builder().tsMillis(1000).content("log1").size(10).build());
    var sealed = bp.snapshotSealed(sid, 0, Long.MAX_VALUE);
    assertEquals(0, sealed.size());
    bp.flushPagesOlderThan(2000);
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(() -> 1 == pageFlusher.getFlushedPages().size());
    var sealedAfterFlush = bp.snapshotSealed(sid, 0, Long.MAX_VALUE);
    assertEquals(1, sealedAfterFlush.size());
    bp.close();
  }

  @Test
  public void testFlushAllNow() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 1024), pageFlusher, 10, 100);
    // append content that will fill the page
    var sid = new LogStreamIdentifier("stream1");
    bp.append(
        mockLsn, sid, MockPageInput.builder().tsMillis(1000).content("log1").size(10).build());
    var sealed = bp.snapshotSealed(sid, 0, Long.MAX_VALUE);
    assertEquals(0, sealed.size());
    bp.flushAllNow();
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(() -> 1 == pageFlusher.getFlushedPages().size());
    var sealedAfterFlush = bp.snapshotSealed(sid, 0, Long.MAX_VALUE);
    assertEquals(1, sealedAfterFlush.size());
    bp.close();
  }

  @Test
  public void testEmptyPageIsNotSealed() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 1024), pageFlusher, 10, 100);
    // append content that will fill the page
    bp.flushAllNow();
    Assertions.assertEquals(0, pageFlusher.getFlushedPages().size());
    bp.close();
  }

  @Test
  public void testEmptyPageNotSealedOnFlushOlder() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 1024), pageFlusher, 10, 100);
    var boundaryTs = System.currentTimeMillis() + 1_000_000_000L;
    pageFlusher.setThrowOnFlush(true);
    bp.flushPagesOlderThan(boundaryTs);
    bp.close();
  }

  @Test
  public void testNonEmptyPageNotSealedOnFlushOlder() throws Exception {
    var pageFlusher = new MockPageFlusher();
    var bp = new BufferPool<>(new MockPageFactory(1000, 1024), pageFlusher, 10, 100);
    var sid = new LogStreamIdentifier("stream1");
    bp.append(
        mockLsn, sid, MockPageInput.builder().tsMillis(1000).content("log1").size(10).build());
    var boundaryTs = System.currentTimeMillis() + 1_000_000_000L;
    bp.flushPagesOlderThan(boundaryTs);
    await()
        .atMost(Duration.ofSeconds(1))
        .until(
            () -> {
              return pageFlusher.flushedPages.size() == 1;
            });
    bp.close();
  }
}

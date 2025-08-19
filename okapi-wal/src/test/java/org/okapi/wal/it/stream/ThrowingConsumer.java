package org.okapi.wal.it.stream;

import org.okapi.wal.Wal;
import org.okapi.wal.it.metric.MetricEventInMemoryStore;
import org.okapi.wal.WalStreamConsumer;

/** Wraps MetricEventInMemoryStore but throws on a specific LSN once. */
final class ThrowingConsumer implements WalStreamConsumer {
  private final MetricEventInMemoryStore delegate = new MetricEventInMemoryStore();
  private final long failAtLsn;
  private boolean thrown = false;

  ThrowingConsumer(long failAtLsn) {
    this.failAtLsn = failAtLsn;
  }

  @Override
  public void consume(long lsn, Wal.WalRecord record) throws Exception {
    if (!thrown && lsn == failAtLsn) {
      thrown = true;
      throw new RuntimeException("Injected failure at LSN " + lsn);
    }
    delegate.consume(lsn, record);
  }

  @Override
  public long lastAppliedLsn() {
    return delegate.lastAppliedLsn();
  }

  @Override
  public void flush() throws Exception {
    // no-op
  }

  long totalEvents() {
    return delegate.totalEvents();
  }
}

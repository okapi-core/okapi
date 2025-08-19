package org.okapi.wal.it.env;

import java.io.IOException;
import java.time.Duration;
import org.okapi.wal.*;
import org.okapi.wal.SpilloverWalWriter.FsyncPolicy;

public final class WriterBuilder {
  private WalAllocator allocator;
  private WalFramer framer;
  private long maxSegment = 1_024 * 1_024;
  private WalCommitListener listener = null;
  private FsyncPolicy fsyncPolicy = FsyncPolicy.MANUAL;
  private long fsyncBytes = 0L;
  private Duration fsyncInterval = null;

  public WriterBuilder allocator(WalAllocator a) {
    this.allocator = a;
    return this;
  }

  public WriterBuilder framer(WalFramer f) {
    this.framer = f;
    return this;
  }

  public WriterBuilder maxSegmentSize(long s) {
    this.maxSegment = s;
    return this;
  }

  public WriterBuilder commitListener(WalCommitListener l) {
    this.listener = l;
    return this;
  }

  public WriterBuilder fsyncPolicy(FsyncPolicy p) {
    this.fsyncPolicy = p;
    return this;
  }

  public WriterBuilder fsyncBytes(long b) {
    this.fsyncBytes = b;
    return this;
  }

  public WriterBuilder fsyncInterval(Duration d) {
    this.fsyncInterval = d;
    return this;
  }

  public SpilloverWalWriter build() throws IOException {
    if (allocator == null || framer == null) {
      throw new IllegalStateException("allocator and framer must be set");
    }
    return new SpilloverWalWriter(
        allocator, framer, maxSegment, listener, fsyncPolicy, fsyncBytes, fsyncInterval);
  }
}

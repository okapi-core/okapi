package org.okapi.traces.page;

/**
 * Flushes pages when their estimated serialized size crosses a threshold.
 * Default threshold suggestion: 8 MB (or a multiple of the system page size).
 */
public class SizeBasedFlushStrategy implements FlushStrategy {
  public static final long DEFAULT_MAX_BYTES = 8L * 1024L * 1024L; // 8 MB

  private final long maxEstimatedBytes;

  public SizeBasedFlushStrategy(long maxEstimatedBytes) {
    this.maxEstimatedBytes = maxEstimatedBytes;
  }

  @Override
  public boolean shouldFlush(SpanPage page) {
    long est = page.estimatedSerializedSizeBytes();
    return est >= 0 && est >= maxEstimatedBytes;
  }
}


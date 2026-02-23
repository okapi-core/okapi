package org.okapi.traces.io;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.AccessLevel;
import lombok.Getter;
import org.okapi.pages.AbstractTimeBlockMetadata;

public class SpanPageMetadata extends AbstractTimeBlockMetadata {
  @Getter(AccessLevel.PACKAGE)
  BloomFilter<byte[]> traceIdFilter;

  @Getter(AccessLevel.PACKAGE)
  BloomFilter<byte[]> spanIdFilter;

  public SpanPageMetadata(
      long tsStart,
      long tsEnd,
      BloomFilter<byte[]> traceIdFilter,
      BloomFilter<byte[]> spanIdFilter) {
    super();
    setTsStart(tsStart);
    setTsEnd(tsEnd);
    this.traceIdFilter = traceIdFilter;
    this.spanIdFilter = spanIdFilter;
  }

  public static SpanPageMetadata makeEmpty(int expectedInsertions, double fpp) {
    return new SpanPageMetadata(
        0,
        0,
        BloomFilter.create(Funnels.byteArrayFunnel(), expectedInsertions, fpp),
        BloomFilter.create(Funnels.byteArrayFunnel(), expectedInsertions, fpp));
  }

  void putTraceId(byte[] traceId) {
    traceIdFilter.put(traceId);
  }

  void putSpanId(byte[] spanId) {
    spanIdFilter.put(spanId);
  }

  public SpanPageMetadataSnapshot snapshot() {
    return new SpanPageMetadataSnapshot(getTsStart(), getTsEnd(), traceIdFilter);
  }

  public boolean maybeContainsTraceId(byte[] traceId) {
    return traceIdFilter.mightContain(traceId);
  }

  public boolean maybeContainsSpanId(byte[] spanId) {
    return spanIdFilter.mightContain(spanId);
  }
}

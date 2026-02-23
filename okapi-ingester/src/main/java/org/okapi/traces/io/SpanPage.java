/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.io;

import lombok.Builder;
import org.okapi.logs.io.AbstractTimestampedPage;
import org.okapi.pages.AbstractTimeBlockMetadata;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.wal.lsn.Lsn;

public final class SpanPage extends AbstractTimestampedPage
    implements AppendOnlyPage<
        SpanIngestionRecord, SpanPageSnapshot, SpanPageMetadata, SpanPageBody> {
  private final long maxRangeMs;
  private final long maxSizeBytes;

  // Expose internals used by codecs and processors
  SpanPageMetadata spanPageMetadata;

  SpanPageBody spanPageBody;

  private long estimatedBytes = 80 + 4; // header + CRC baseline

  @Builder
  public SpanPage(int expectedInsertions, double fpp, long maxRangeMs, long maxSizeBytes) {
    this.spanPageBody = new SpanPageBody();
    this.spanPageMetadata = SpanPageMetadata.makeEmpty(expectedInsertions, fpp);
    this.maxRangeMs = maxRangeMs;
    this.maxSizeBytes = maxSizeBytes;
  }

  public SpanPage(SpanPageMetadata pageMetadata, SpanPageBody spanPageBody) {
    this.spanPageMetadata = pageMetadata;
    this.spanPageBody = spanPageBody;
    this.maxRangeMs = 0;
    this.maxSizeBytes = 0;
    this.estimatedBytes = 0;
  }

  @Override
  public void append(SpanIngestionRecord req) {
    var spanRecord = BinarySpanRecordV2.fromIngestionRecord(req);
    this.spanPageBody.add(spanRecord);
    var traceId = spanRecord.getSpan().getTraceId().toByteArray();
    var spanId = spanRecord.getSpan().getSpanId().toByteArray();
    var sp = req.getSpan();
    this.spanPageMetadata.putTraceId(traceId);
    this.spanPageMetadata.putSpanId(spanId);
    this.spanPageMetadata.updateTsStart(sp.getStartTimeUnixNano() / 1_000_000L);
    this.spanPageMetadata.updateTsEnd(sp.getEndTimeUnixNano() / 1_000_000L);
    this.estimatedBytes += sp.getSerializedSize();
  }

  @Override
  public AbstractTimeBlockMetadata getBlockMetadata() {
    return this.spanPageMetadata;
  }

  @Override
  public boolean isFull() {
    boolean sizeFull = estimatedBytes >= maxSizeBytes;
    return sizeFull || isTimeRangeFull(this.maxRangeMs);
  }

  @Override
  public boolean isEmpty() {
    return getPageBody().size() == 0;
  }

  @Override
  public SpanPageSnapshot snapshot() {
    return new SpanPageSnapshot(spanPageMetadata.snapshot(), spanPageBody.toSnapshot());
  }

  @Override
  public SpanPageMetadata getMetadata() {
    return spanPageMetadata;
  }

  @Override
  public SpanPageBody getPageBody() {
    return spanPageBody;
  }

  @Override
  public Lsn getMaxLsn() {
    return Lsn.fromNumber(this.spanPageMetadata.getMaxLsn());
  }

  @Override
  public void updateLsn(Lsn lsn) {
    this.spanPageMetadata.setMaxLsn(lsn.getNumber());
  }
}

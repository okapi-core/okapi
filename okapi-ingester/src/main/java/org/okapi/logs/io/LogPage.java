/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import com.google.common.base.Preconditions;
import java.util.*;
import lombok.*;
import org.okapi.abstractio.TrigramUtil;
import org.okapi.pages.AbstractTimeBlockMetadata;
import org.okapi.pages.AppendOnlyPage;
import org.okapi.wal.lsn.Lsn;

@Getter
@ToString
public class LogPage extends AbstractTimestampedPage
    implements AppendOnlyPage<LogIngestRecord, LogPageSnapshot, LogPageMetadata, LogPageBody> {
  private final LogPageMetadata logPageMetadata;
  private final LogPageBody pageBody;

  // Limits
  private final long maxRangeMs;
  private final int maxBodyBytes;
  // Approximate serialized size estimator (header + CRC to start)
  private int bodySize;

  @Builder
  public LogPage(Integer expectedInsertions, Long maxRangeMs, int maxSizeBytes) {
    // Ensure mutable collections
    Preconditions.checkNotNull(expectedInsertions);
    Preconditions.checkNotNull(maxRangeMs);
    this.logPageMetadata = LogPageMetadata.createEmptyMetadata(expectedInsertions);
    this.pageBody = new LogPageBody();
    this.maxRangeMs = maxRangeMs;
    this.maxBodyBytes = maxSizeBytes;
  }

  protected LogPage(LogPageMetadata logPageMetadata, LogPageBody pageBody) {
    this.logPageMetadata = logPageMetadata;
    this.pageBody = pageBody;
    this.maxRangeMs = -1;
    this.maxBodyBytes = -1;
    this.bodySize = -1;
  }

  public int getNDocs() {
    return pageBody.size();
  }

  public boolean maybeContainsLeveInPage(int level) {
    return this.logPageMetadata.maybeContainsLeveInPage(level);
  }

  public boolean maybeContainsTrigram(int trigram) {
    return this.logPageMetadata.maybeContainsTrigram(trigram);
  }

  public boolean maybeContainsTraceId(String traceId) {
    return this.logPageMetadata.maybeContainsTraceId(traceId);
  }

  @Override
  public void append(LogIngestRecord rec) {
    long tsMillis = rec.tsMillis();
    String traceId = rec.traceId();
    int level = rec.level();
    String body = rec.body();
    logPageMetadata.updateTsStart(tsMillis);
    logPageMetadata.updateTsEnd(tsMillis);

    var payload = LogRecordTranslator.toBinaryRecord(rec);
    this.pageBody.add(payload);

    // levels -> docIds
    this.logPageMetadata.putLogLevel(rec.level());
    if (traceId != null) {
      this.logPageMetadata.putTraceId(traceId);
    }
    this.logPageMetadata.putLogLevel(level);
    // trigrams
    for (int tri : TrigramUtil.extractAsciiTrigramIndices(body)) {
      this.logPageMetadata.putLogBodyTrigram(tri);
    }

    bodySize += payload.getSerializedSize();
  }

  @Override
  public boolean isFull() {
    boolean sizeFull = bodySize >= maxBodyBytes;
    boolean timeFull = (logPageMetadata.getTsEnd() - logPageMetadata.getTsStart()) >= maxRangeMs;
    return sizeFull || timeFull;
  }

  @Override
  public boolean isEmpty() {
    return getNDocs() == 0;
  }

  @Override
  public LogPageSnapshot snapshot() {
    return new LogPageSnapshot(logPageMetadata.toSnapshot(), pageBody.toSnapshot());
  }

  @Override
  public LogPageMetadata getMetadata() {
    return logPageMetadata;
  }

  @Override
  public Lsn getMaxLsn() {
    return Lsn.fromNumber(this.logPageMetadata.getMaxLsn());
  }

  @Override
  public void updateLsn(Lsn lsn) {
    this.logPageMetadata.setMaxLsn(lsn.getNumber());
  }

  public long getTsStart() {
    return logPageMetadata.getTsStart();
  }

  public long getTsEnd() {
    return logPageMetadata.getTsEnd();
  }

  @Override
  public AbstractTimeBlockMetadata getBlockMetadata() {
    return this.logPageMetadata;
  }
}

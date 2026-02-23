package org.okapi.logs.io;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.Getter;
import org.okapi.pages.AbstractTimeBlockMetadata;

public class LogPageMetadata extends AbstractTimeBlockMetadata {
  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<Integer> logBodyTrigrams;

  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<Integer> logLevels;

  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<CharSequence> traceIdSet;

  protected LogPageMetadata(
      long tsStart,
      long tsEnd,
      BloomFilter<Integer> logLevels,
      BloomFilter<CharSequence> traceIdSet,
      BloomFilter<Integer> logBodyTrigrams) {
    setTsStart(tsStart);
    setTsEnd(tsEnd);
    this.logBodyTrigrams = logBodyTrigrams;
    this.logLevels = logLevels;
    this.traceIdSet = traceIdSet;
  }

  public static LogPageMetadata createEmptyMetadata(Integer expectedInsertions) {
    var logBodyTrigrams = BloomFilter.create(Funnels.integerFunnel(), expectedInsertions);
    var traceIdSet =
        BloomFilter.create(Funnels.stringFunnel(StandardCharsets.US_ASCII), expectedInsertions);
    var logLevels = BloomFilter.create(Funnels.integerFunnel(), expectedInsertions);
    return new LogPageMetadata(0, 0, logLevels, traceIdSet, logBodyTrigrams);
  }

  public boolean maybeContainsLeveInPage(int level) {
    return this.logLevels.mightContain(level);
  }

  public boolean maybeContainsTrigram(int trigram) {
    return this.logBodyTrigrams.mightContain(trigram);
  }

  public boolean maybeContainsTraceId(String traceId) {
    return this.traceIdSet.mightContain(traceId);
  }

  public void putTraceId(String traceId) {
    this.traceIdSet.put(traceId);
  }

  public void putLogLevel(int level) {
    this.logLevels.put(level);
  }

  public void putLogBodyTrigram(int trigram) {
    this.logBodyTrigrams.put(trigram);
  }

  public LogPageMetadataSnapshot toSnapshot() {
    return new LogPageMetadataSnapshot(
        getTsStart(), getTsEnd(), logBodyTrigrams, logLevels, traceIdSet);
  }
}

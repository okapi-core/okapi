package org.okapi.logs.io;

import com.google.common.hash.BloomFilter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class LogPageMetadataSnapshot {
  @Getter private long tsStart;
  @Getter private long tsEnd;
  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<Integer> logBodyTrigrams;
  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<Integer> logLevels;
  @Getter(AccessLevel.PACKAGE)
  private final BloomFilter<CharSequence> traceIdSet;
}

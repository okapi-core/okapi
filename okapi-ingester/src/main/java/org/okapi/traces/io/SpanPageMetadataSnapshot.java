package org.okapi.traces.io;

import com.google.common.hash.BloomFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SpanPageMetadataSnapshot {
  @Getter long tsStart;
  @Getter long tsEnd;
  BloomFilter<byte[]> traceIdFilter;
}

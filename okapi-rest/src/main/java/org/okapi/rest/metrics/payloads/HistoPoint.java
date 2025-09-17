package org.okapi.rest.metrics.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
@NoArgsConstructor
public final class HistoPoint {
  long start;
  long end;
  float[] buckets;
  // bucketCounts.length = 1 + buckets.length
  int[] bucketCounts;
}

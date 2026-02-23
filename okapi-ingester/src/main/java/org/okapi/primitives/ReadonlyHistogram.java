package org.okapi.primitives;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class ReadonlyHistogram {
  public enum TEMPORALITY {
    DELTA,
    CUMULATIVE
  }

  @Getter long startTs;
  @Getter Long endTs;
  @Getter Histogram.TEMPORALITY temporality;
  int[] bucketCounts;
  float[] buckets;

  public List<Integer> getBucketCounts() {
    return new UnmodifiableIntegerList(bucketCounts);
  }

  public List<Float> getBuckets() {
    return new UnmodifiableDoubleList(buckets);
  }
}

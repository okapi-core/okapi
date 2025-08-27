package org.okapi.metrics.rollup;

import java.util.Optional;
import org.okapi.collections.StringUtils;

public class HashFns {

  enum HASH_TYPE {
    s,
    m,
    h
  }

  public record HashedValue(String timeSeries, long ts, HASH_TYPE hashType) {}

  public static String hourlyBucket(String timeSeries, long ts) {
    return (ts / 1000 / 3600) + ":h:" + timeSeries;
  }

  public static String minutelyBucket(String timeSeries, long ts) {
    return (ts / 1000 / 60) + ":m:" + timeSeries;
  }

  public static String secondlyBucket(String timeSeries, long ts) {
    return (ts / 1000) + ":s:" + timeSeries;
  }

  public static Optional<HashedValue> invertKey(String key) {
    var splits = StringUtils.splitLeft(key, ':', 2);

    if (splits.size() != 3) {
      return Optional.empty();
    }
    var hashType = HASH_TYPE.valueOf(splits.get(1));
    var time = Long.parseLong(splits.get(0));
    var series = splits.get(2);
    return Optional.of(new HashedValue(series, time, hashType));
  }
}

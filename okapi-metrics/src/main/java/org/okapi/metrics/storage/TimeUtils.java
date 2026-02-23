package org.okapi.metrics.storage;

public class TimeUtils {

  public static long roundDownToHour(long ts) {
    return ts / 1000 / 3600;
  }

  public static long roundUpToHour(long ts) {
    return 1 + roundDownToHour(ts);
  }
}

package org.okapi.traces.util;

public final class TimeBuckets {
  private TimeBuckets() {}

  public static long secondBucket(long epochMillis) {
    return epochMillis / 1000L;
  }

  public static long minuteBucket(long epochMillis) {
    return epochMillis / 60000L; // minutes since epoch
  }
}


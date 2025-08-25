package org.okapi.metrics.rollup;

public class HashFns {

  public static String hourlyBucket(String timeSeries, long ts) {
    return timeSeries + ":h:" + (ts / 1000 / 3600);
  }

  public static String minutelyBucket(String timeSeries, long ts) {
    return timeSeries + ":m:" + (ts / 1000 / 60);
  }

  public static String secondlyBucket(String timeSeries, long ts) {
    return timeSeries + ":s:" + (ts / 1000);
  }
}

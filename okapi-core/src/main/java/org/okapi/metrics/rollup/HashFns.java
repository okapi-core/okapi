package org.okapi.metrics.rollup;

public class HashFns {

  // todo: add utility to get range for hourly cleanup -> make sure to compact
  // todo: make frozen writer tests pass
  public static String hourlyBucket(String timeSeries, long ts) {
    return (ts / 1000 / 3600) + ":h:" + timeSeries;
  }

  public static String minutelyBucket(String timeSeries, long ts) {
    return (ts / 1000 / 60) + ":m:" + timeSeries;
  }

  public static String secondlyBucket(String timeSeries, long ts) {
    return (ts / 1000) + ":s:" + timeSeries;
  }
}

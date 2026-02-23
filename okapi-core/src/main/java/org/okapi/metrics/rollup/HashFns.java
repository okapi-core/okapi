/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.rollup;

public class HashFns {

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

package org.okapi.metrics.service;

import com.google.common.base.Preconditions;

import java.time.Duration;
import java.util.Random;

public class ExponentialBackoffCalculator {

  public static Duration wait(Duration base, int multiplier, int trial) {
    Preconditions.checkArgument(trial > 0, "Trial cannot be negative");
    var random = new Random();
    var maxWaitDuration = base.toMillis() * Math.pow(multiplier, trial);
    var wait = (long) (random.nextDouble() * maxWaitDuration);
    return Duration.ofMillis(wait);
  }

  public static long jitteryWait(int min, int max) {
    Preconditions.checkArgument(min > 0 && max > 0, "Both min and max should be positive");
    Preconditions.checkArgument(max > min, "Max time should be greater than min.");
    var random = new Random();
    return (long) (min + (max - min) * (random.nextDouble()));
  }
}

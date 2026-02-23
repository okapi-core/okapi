package org.okapi.metrics.service;

import com.google.common.base.Preconditions;
import java.util.Random;

public class ExponentialBackoffCalculator {

  public static long jitteryWait(int min, int max) {
    Preconditions.checkArgument(min > 0 && max > 0, "Both min and max should be positive");
    Preconditions.checkArgument(max > min, "Max time should be greater than min.");
    var random = new Random();
    return (long) (min + (max - min) * (random.nextDouble()));
  }
}

package org.okapi.fixtures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Percentiles {

  public static float getPercentile(List<Float> ref, double percentile) {
    var copy = new ArrayList<>(ref);
    Collections.sort(copy);
    var idx = percentile * copy.size();
    var lower = (int) idx;
    if (lower == ref.size()) {
      return copy.get(lower - 1);
    }
    if (lower == idx) {
      return copy.get(lower);
    }
    var upper = 1 + lower;
    var fractional = (float) (idx - lower);
    if (upper < copy.size()) {
      return (1 - fractional) * copy.get(lower) + fractional * copy.get(upper);
    } else {
      return copy.get(lower);
    }
  }
}

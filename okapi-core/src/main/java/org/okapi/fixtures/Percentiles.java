package org.okapi.fixtures;

import java.util.List;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class Percentiles {

  public static float getPercentile(List<Float> ref, double percentile) {
    //    var copy = new ArrayList<>(ref);
    //    Collections.sort(copy);
    //    var idx = percentile * copy.size();
    //    var lower = (int) idx;
    //    if (lower == ref.size()) {
    //      return copy.get(lower - 1);
    //    }
    //    if (lower == idx) {
    //      return copy.get(lower);
    //    }
    //    var upper = 1 + lower;
    //    var fractional = (float) (idx - lower);
    //    if (upper < copy.size()) {
    //      return (1 - fractional) * copy.get(lower) + fractional * copy.get(upper);
    //    } else {
    //      return copy.get(lower);
    //    }

    assert percentile < 1.0;
    var vals = ref.stream().mapToDouble(Float::doubleValue).toArray();
    var percentiler = new Percentile(100. * percentile);
    return (float) percentiler.evaluate(vals);
  }
}

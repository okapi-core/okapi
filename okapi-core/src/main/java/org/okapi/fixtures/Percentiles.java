package org.okapi.fixtures;

import java.util.List;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

public class Percentiles {

  public static float getPercentile(List<Float> ref, double percentile) {
    assert percentile < 1.0;
    var vals = ref.stream().mapToDouble(Float::doubleValue).toArray();
    var percentiler = new Percentile(100. * percentile);
    return (float) percentiler.evaluate(vals);
  }
}

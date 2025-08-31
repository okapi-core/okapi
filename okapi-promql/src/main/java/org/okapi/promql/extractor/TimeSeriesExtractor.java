package org.okapi.promql.extractor;

import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.VectorData;

public class TimeSeriesExtractor {
  public static float findValue(InstantVectorResult iv, VectorData.SeriesId series, long ts) {
    return iv.data().stream()
        .filter(s -> s.series().equals(series) && s.sample().ts() == ts)
        .findFirst()
        .orElseThrow(() -> new AssertionError("Missing sample for " + series + " @ " + ts))
        .sample()
        .value();
  }
}

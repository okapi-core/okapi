package org.okapi.promql.eval.ts;

import org.okapi.promql.eval.VectorData.SeriesId;
import org.okapi.promql.parse.*;

import java.util.List;

public interface SeriesDiscovery {
  /** Expand a selector into concrete (metric, tags) series. */
  List<SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers, long start, long end);
}

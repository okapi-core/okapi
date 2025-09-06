package org.okapi.metrics.fdb;

import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;

public class SubspaceFactory {
  public static final String[] METRICS_SPACE = {"okapi", "metrics"};
  public static final String[] METRICS_SEARCH = {"okapi", "metrics-search"};

  public static Subspace getMetricsSub() {
    var subspace = new Subspace(Tuple.from(METRICS_SPACE[0], METRICS_SPACE[1]));
    return subspace;
  }

  public static Subspace getMetricsSearchSub() {
    var subspace = new Subspace(Tuple.from(METRICS_SEARCH[0], METRICS_SEARCH[1]));
    return subspace;
  }
}

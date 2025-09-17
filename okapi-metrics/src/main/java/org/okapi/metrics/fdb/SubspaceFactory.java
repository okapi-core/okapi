package org.okapi.metrics.fdb;

import com.apple.foundationdb.subspace.Subspace;
import com.apple.foundationdb.tuple.Tuple;

public class SubspaceFactory {
  public static final String[] METRICS_SPACE = {"okapi", "metrics"};
  public static final String[] METRICS_SEARCH = {"okapi", "metrics-search"};
  public static final String METRICS_SUM = "sum";
  public static final String METRICS_GAUGE = "gauge";
  public static final String METRICS_CSUM = "csum";
  public static final String METRICS_HISTO = "histo";

  public static Subspace getGaugeSub() {
    var subspace = new Subspace(Tuple.from(METRICS_SPACE[0], METRICS_SPACE[1], METRICS_GAUGE));
    return subspace;
  }

  public static Subspace getMetricsSearchSub() {
    var subspace = new Subspace(Tuple.from(METRICS_SEARCH[0], METRICS_SEARCH[1]));
    return subspace;
  }

  public static Subspace getSumSubspace() {
    var subspace = new Subspace(Tuple.from(METRICS_SPACE[0], METRICS_SPACE[1], METRICS_SUM));
    return subspace;
  }

  public static Subspace getCsumSubspace() {
    var subspace = new Subspace(Tuple.from(METRICS_SPACE[0], METRICS_SPACE[1], METRICS_CSUM));
    return subspace;
  }

  public static Subspace getHistoSubspace() {
    var subspace = new Subspace(Tuple.from(METRICS_SPACE[0], METRICS_SPACE[1], METRICS_HISTO));
    return subspace;
  }
}

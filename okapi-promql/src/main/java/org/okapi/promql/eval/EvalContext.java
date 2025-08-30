package org.okapi.promql.eval;

// eval/EvalContext.java
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.eval.ts.TimeseriesClient;

import java.util.concurrent.*;

public final class EvalContext {
  public final long startMs, endMs, stepMs;
  public final RESOLUTION resolution;
  public final TimeseriesClient client;
  public final SeriesDiscovery discovery;
  public final ExecutorService exec;

  public EvalContext(
      long startMs,
      long endMs,
      long stepMs,
      RESOLUTION res,
      TimeseriesClient client,
      SeriesDiscovery discovery,
      ExecutorService exec) {
    this.startMs = startMs;
    this.endMs = endMs;
    this.stepMs = stepMs;
    this.resolution = res;
    this.client = client;
    this.discovery = discovery;
    this.exec = exec;
  }
}

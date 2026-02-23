package org.okapi.promql.eval;

import java.util.concurrent.*;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.eval.ts.TsClient;

public final class EvalContext {
  public final long startMs, endMs, stepMs;
  public final RESOLUTION resolution;
  public final TsClient client;
  public final SeriesDiscovery discovery;
  public final ExecutorService exec;

  public EvalContext(
      long startMs,
      long endMs,
      long stepMs,
      RESOLUTION res,
      TsClient client,
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

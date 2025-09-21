package org.okapi.promql.eval.ops;

import java.util.ArrayList;
import java.util.List;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.Scan;

public final class InstantizeEval implements Evaluable {
  private static final long DEFAULT_STALENESS_MS = 5 * 60_000L;
  private final Evaluable inner;

  public InstantizeEval(Evaluable inner) {
    this.inner = inner;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    var res = inner.eval(ctx);
    if (!(res instanceof RangeVectorResult rv)) {
      // already instant? then just pass through
      return res;
    }
    List<VectorData.SeriesSample> out = new ArrayList<>();
    for (VectorData.SeriesWindow w : rv.data()) {
      Scan s = w.scan();
      if (s instanceof GaugeScan gs) {
        var tsList = gs.getTimestamps();
        var valList = gs.getValues();
        int n = tsList.size();
        int idx = 0;
        for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
          long winStart = t - DEFAULT_STALENESS_MS;
          while (idx + 1 < n && tsList.get(idx + 1) <= t) idx++;
          if (n == 0) continue;
          long ptsTs = tsList.get(idx);
          if (ptsTs <= t && ptsTs > winStart) {
            float v = valList.get(idx);
            out.add(new VectorData.SeriesSample(w.id(), new VectorData.Sample(t, v)));
          }
        }
      }
      // For non-gauge scans, instantize is not defined here; skip.
    }
    return new InstantVectorResult(out);
  }
}

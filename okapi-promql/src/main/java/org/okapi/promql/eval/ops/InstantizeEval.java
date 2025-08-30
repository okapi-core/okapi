package org.okapi.promql.eval.ops;

import java.util.ArrayList;
import java.util.List;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.exceptions.EvaluationException;

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
      var pts = w.points();
      int n = pts.size();
      int idx = 0;
      for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
        long winStart = t - DEFAULT_STALENESS_MS;
        // advance idx to last point <= t
        while (idx + 1 < n && pts.get(idx + 1).ts() <= t) idx++;
        if (n == 0) continue;
        var p = pts.get(idx);
        if (p.ts() <= t && p.ts() > winStart) {
          // choose an instantaneous value; use avg() as point value
          float v = p.stats().avg();
          out.add(new VectorData.SeriesSample(w.id(), new VectorData.Sample(t, v)));
        }
      }
    }
    return new InstantVectorResult(out);
  }
}

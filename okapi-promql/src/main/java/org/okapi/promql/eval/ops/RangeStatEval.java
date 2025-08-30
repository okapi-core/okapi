package org.okapi.promql.eval.ops;

// eval/ops/RangeStatEval.java
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;
import org.okapi.promql.eval.nodes.RangeSelectorExpr;
import org.okapi.promql.eval.nodes.SubqueryExpr;
import org.okapi.promql.eval.ts.StatisticsMerger;

@AllArgsConstructor
public class RangeStatEval implements Evaluable {
  enum Kind {
    AVG,
    MIN,
    MAX,
    SUM,
    COUNT,
    QUANTILE,
    LAST,
    PRESENT
  }

  private final FunctionExpr fn;
  private final Kind kind;
  private final StatisticsMerger merger;

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    int argIdx = 0;
    float q = Float.NaN;
    if (kind == Kind.QUANTILE) {
      if (fn.args.size() != 2)
        throw new IllegalArgumentException("quantile_over_time(q, range-vector)");
      var qRes = fn.args.get(0).lower().eval(ctx);
      if (!(qRes instanceof ScalarResult s)) throw new IllegalArgumentException("q must be scalar");
      q = s.value;
      argIdx = 1;
    }
    var vec = fn.args.get(argIdx).lower().eval(ctx);
    if (!(vec instanceof RangeVectorResult rv))
      throw new IllegalArgumentException(kind + " expects range vector");

    long rangeMs = inferRange(fn.args.get(argIdx));
    List<SeriesSample> out = new ArrayList<>();
    for (SeriesWindow w : rv.data()) {
      var pts = w.points();
      for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
        long winStart = t - rangeMs;
        Statistics agg = null;
        int count = 0;
        StatsPoint lastPoint = null;

        for (StatsPoint p : pts) {
          if (p.ts() <= winStart || p.ts() > t) continue; // (start, t]
          agg = (agg == null) ? p.stats() : merger.merge(agg, p.stats());
          count++;
          lastPoint = p;
        }

        float value =
            switch (kind) {
              case AVG -> agg != null ? agg.avg() : Float.NaN;
              case MIN -> agg != null ? agg.min() : Float.NaN;
              case MAX -> agg != null ? agg.max() : Float.NaN;
              case SUM -> agg != null ? (float) agg.getSum() : 0f;
              case COUNT -> (float) count;
              case QUANTILE -> agg != null ? agg.percentile(q) : Float.NaN;
              case LAST -> lastPoint != null ? lastPoint.stats().avg() : Float.NaN;
              case PRESENT -> (count > 0 ? 1f : 0f);
            };
        out.add(new SeriesSample(w.id(), new Sample(t, value)));
      }
    }
    return new InstantVectorResult(out);
  }

  private long inferRange(LogicalExpr arg) {
    if (arg instanceof RangeSelectorExpr r) return r.rangeMs;
    if (arg instanceof SubqueryExpr sq) return sq.rangeMs;
    throw new IllegalStateException("Range function requires range selector or subquery");
  }
}

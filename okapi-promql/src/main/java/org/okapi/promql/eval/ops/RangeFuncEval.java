package org.okapi.promql.eval.ops;

import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;
import org.okapi.promql.eval.nodes.RangeSelectorExpr;
import org.okapi.promql.eval.nodes.SubqueryExpr;

@AllArgsConstructor
public class RangeFuncEval implements Evaluable {
  enum Kind {
    RATE,
    IRATE,
    INCREASE,
    DELTA,
    IDELTA,
    DERIV
  }

  private final FunctionExpr fn;
  private final Kind kind;

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 1) throw new IllegalArgumentException(kind + " expects one arg");
    var argRes = fn.args.get(0).lower().eval(ctx);
    if (!(argRes instanceof RangeVectorResult rv))
      throw new IllegalArgumentException(kind + " expects range vector");

    long rangeMs = inferRangeFromArg(fn.args.get(0));
    List<SeriesSample> out = new ArrayList<>();
    for (SeriesWindow w : rv.data()) {
      var pts = w.points();
      for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
        long winStart = t - rangeMs;
        float value =
            switch (kind) {
              case INCREASE -> increase(pts, winStart, t);
              case RATE -> {
                float inc = increase(pts, winStart, t);
                yield (t > winStart) ? inc / ((t - winStart) / 1000f) : Float.NaN;
              }
              case IRATE -> irate(pts, winStart, t);
              case DELTA -> delta(pts, winStart, t);
              case IDELTA -> idelta(pts, winStart, t);
              case DERIV -> deriv(pts, winStart, t);
            };
        out.add(new SeriesSample(w.id(), new Sample(t, value)));
      }
    }
    return new InstantVectorResult(out);
  }

  private static float increase(List<StatsPoint> pts, long start, long end) {
    float total = 0f;
    for (var p : pts) {
      long ts = p.ts();
      if (ts <= start || ts > end) continue; // (start, end]
      total += (float) p.stats().getSum();
    }
    return total;
  }

  private static float irate(List<StatsPoint> pts, long start, long end) {
    StatsPoint prev = null, last = null;
    for (int i = pts.size() - 1; i >= 0; --i) {
      var p = pts.get(i);
      if (p.ts() <= start || p.ts() > end) continue;
      if (last == null) last = p;
      else {
        prev = p;
        break;
      }
    }
    if (last == null || prev == null) return Float.NaN;
    float delta = (float) last.stats().getSum();
    float seconds = Math.max((last.ts() - prev.ts()) / 1000f, 1f);
    return delta / seconds;
  }

  private static float delta(List<StatsPoint> pts, long start, long end) {
    StatsPoint first = null, last = null;
    for (var p : pts) {
      if (p.ts() <= start || p.ts() > end) continue;
      if (first == null) first = p;
      last = p;
    }
    if (first == null || last == null) return Float.NaN;
    return last.stats().avg() - first.stats().avg();
  }

  private static float idelta(List<StatsPoint> pts, long start, long end) {
    StatsPoint prev = null, last = null;
    for (int i = pts.size() - 1; i >= 0; --i) {
      var p = pts.get(i);
      if (p.ts() <= start || p.ts() > end) continue;
      if (last == null) last = p;
      else {
        prev = p;
        break;
      }
    }
    if (last == null || prev == null) return Float.NaN;
    return last.stats().avg() - prev.stats().avg();
  }

  private static float deriv(List<StatsPoint> pts, long start, long end) {
    StatsPoint first = null, last = null;
    for (var p : pts) {
      if (p.ts() <= start || p.ts() > end) continue;
      if (first == null) first = p;
      last = p;
    }
    if (first == null || last == null) return Float.NaN;
    float d = last.stats().avg() - first.stats().avg();
    float seconds = Math.max((last.ts() - first.ts()) / 1000f, 1f);
    return d / seconds;
  }

  private long inferRangeFromArg(LogicalExpr arg) {
    if (arg instanceof RangeSelectorExpr r) return r.rangeMs;
    if (arg instanceof SubqueryExpr sq) return sq.rangeMs;
    throw new IllegalStateException("Range function requires range selector or subquery");
  }
}

package org.okapi.promql.eval.ops;

import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.metrics.pojos.results.SumScan;
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
      Scan s = w.scan();
      for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
        long winStart = t - rangeMs;
        float value;
        switch (kind) {
          case INCREASE ->
              value = (s instanceof SumScan ss) ? increase(ss, winStart, t) : Float.NaN;
          case RATE -> {
            if (s instanceof SumScan ss) {
              float inc = increase(ss, winStart, t);
              value = (t > winStart) ? inc / ((t - winStart) / 1000f) : Float.NaN;
            } else value = Float.NaN;
          }
          case IRATE -> value = (s instanceof SumScan ss) ? irate(ss, winStart, t) : Float.NaN;
          case DELTA -> value = (s instanceof GaugeScan gs) ? delta(gs, winStart, t) : Float.NaN;
          case IDELTA -> value = (s instanceof GaugeScan gs) ? idelta(gs, winStart, t) : Float.NaN;
          case DERIV -> value = (s instanceof GaugeScan gs) ? deriv(gs, winStart, t) : Float.NaN;
          default -> value = Float.NaN;
        }
        out.add(new SeriesSample(w.id(), new Sample(t, value)));
      }
    }
    return new InstantVectorResult(out);
  }

  private static float increase(SumScan ss, long start, long end) {
    float total = 0f;
    var ts = ss.getTs();
    var cnt = ss.getCounts();
    for (int i = 0; i < ts.size(); i++) {
      long tsi = ts.get(i);
      if (tsi <= start || tsi > end) continue; // (start, end]
      total += cnt.get(i);
    }
    return total;
  }

  private static float irate(SumScan ss, long start, long end) {
    var ts = ss.getTs();
    var cnt = ss.getCounts();
    Integer lastIdx = null, prevIdx = null;
    for (int i = ts.size() - 1; i >= 0; --i) {
      long tsi = ts.get(i);
      if (tsi <= start || tsi > end) continue;
      if (lastIdx == null) lastIdx = i;
      else {
        prevIdx = i;
        break;
      }
    }
    if (lastIdx == null || prevIdx == null) return Float.NaN;
    float delta = cnt.get(lastIdx);
    float seconds = Math.max((ts.get(lastIdx) - ts.get(prevIdx)) / 1000f, 1f);
    return delta / seconds;
  }

  private static float delta(GaugeScan gs, long start, long end) {
    var ts = gs.getTimestamps();
    var vals = gs.getValues();
    int firstIdx = -1, lastIdx = -1;
    for (int i = 0; i < ts.size(); i++) {
      long tsi = ts.get(i);
      if (tsi <= start || tsi > end) continue;
      if (firstIdx == -1) firstIdx = i;
      lastIdx = i;
    }
    if (firstIdx == -1 || lastIdx == -1) return Float.NaN;
    return vals.get(lastIdx) - vals.get(firstIdx);
  }

  private static float idelta(GaugeScan gs, long start, long end) {
    var ts = gs.getTimestamps();
    var vals = gs.getValues();
    Integer lastIdx = null, prevIdx = null;
    for (int i = ts.size() - 1; i >= 0; --i) {
      long tsi = ts.get(i);
      if (tsi <= start || tsi > end) continue;
      if (lastIdx == null) lastIdx = i;
      else {
        prevIdx = i;
        break;
      }
    }
    if (lastIdx == null || prevIdx == null) return Float.NaN;
    return vals.get(lastIdx) - vals.get(prevIdx);
  }

  private static float deriv(GaugeScan gs, long start, long end) {
    var ts = gs.getTimestamps();
    var vals = gs.getValues();
    int firstIdx = -1, lastIdx = -1;
    for (int i = 0; i < ts.size(); i++) {
      long tsi = ts.get(i);
      if (tsi <= start || tsi > end) continue;
      if (firstIdx == -1) firstIdx = i;
      lastIdx = i;
    }
    if (firstIdx == -1 || lastIdx == -1) return Float.NaN;
    float d = vals.get(lastIdx) - vals.get(firstIdx);
    float seconds = Math.max((ts.get(lastIdx) - ts.get(firstIdx)) / 1000f, 1f);
    return d / seconds;
  }

  private long inferRangeFromArg(LogicalExpr arg) {
    if (arg instanceof RangeSelectorExpr r) return r.rangeMs;
    if (arg instanceof SubqueryExpr sq) return sq.rangeMs;
    throw new IllegalStateException("Range function requires range selector or subquery");
  }
}

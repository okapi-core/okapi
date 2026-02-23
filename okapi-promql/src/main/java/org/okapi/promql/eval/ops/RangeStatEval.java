/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.ops;

// eval/ops/RangeStatEval.java
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;
import org.okapi.promql.eval.nodes.RangeSelectorExpr;
import org.okapi.promql.eval.nodes.SubqueryExpr;
import org.okapi.promql.eval.ts.StatisticsMerger;

@AllArgsConstructor
public class RangeStatEval implements Evaluable {
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
      Scan s = w.scan();
      if (s instanceof GaugeScan gs) {
        var ts = gs.getTimestamps();
        var vals = gs.getValues();
        for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
          long winStart = t - rangeMs;
          // Collect window values
          float sum = 0f;
          int count = 0;
          float min = Float.POSITIVE_INFINITY;
          float max = Float.NEGATIVE_INFINITY;
          List<Float> windowVals = (kind == Kind.QUANTILE) ? new ArrayList<>() : null;
          int n = ts.size();
          for (int i = 0; i < n; i++) {
            long tsi = ts.get(i);
            if (tsi <= winStart || tsi > t) continue;
            float v = vals.get(i);
            sum += v;
            count++;
            if (v < min) min = v;
            if (v > max) max = v;
            if (windowVals != null) windowVals.add(v);
          }
          float value;
          switch (kind) {
            case AVG:
              value = count > 0 ? sum / count : Float.NaN;
              break;
            case MIN:
              value = count > 0 ? min : Float.NaN;
              break;
            case MAX:
              value = count > 0 ? max : Float.NaN;
              break;
            case SUM:
              value = sum;
              break;
            case COUNT:
              value = count;
              break;
            case QUANTILE:
              if (windowVals == null || windowVals.isEmpty()) value = Float.NaN;
              else {
                windowVals.sort(Float::compare);
                double rank = q * (windowVals.size() - 1);
                int lo = (int) Math.floor(rank);
                int hi = (int) Math.ceil(rank);
                if (lo == hi) value = windowVals.get(lo);
                else {
                  float lv = windowVals.get(lo);
                  float hv = windowVals.get(hi);
                  float wgt = (float) (rank - lo);
                  value = lv + wgt * (hv - lv);
                }
              }
              break;
            case LAST:
              // last observed value in window
              value = Float.NaN;
              for (int i = ts.size() - 1; i >= 0; --i) {
                long tsi = ts.get(i);
                if (tsi <= winStart || tsi > t) continue;
                value = vals.get(i);
                break;
              }
              break;
            case PRESENT:
              value = (count > 0) ? 1f : 0f;
              break;
            default:
              value = Float.NaN;
          }
          out.add(new SeriesSample(w.id(), new Sample(t, value)));
        }
      } else {
        // Non-gauge scans: range stat functions are not defined here; emit NaN per step.
        for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
          out.add(new SeriesSample(w.id(), new Sample(t, Float.NaN)));
        }
      }
    }
    return new InstantVectorResult(out);
  }

  private long inferRange(LogicalExpr arg) {
    if (arg instanceof RangeSelectorExpr r) return r.rangeMs;
    if (arg instanceof SubqueryExpr sq) return sq.rangeMs;
    throw new IllegalStateException("Range function requires range selector or subquery");
  }

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
}

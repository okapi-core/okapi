package org.okapi.promql.eval.ops;

import java.util.*;
import org.okapi.metrics.pojos.results.HistoScan;
import org.okapi.metrics.pojos.results.HistoScanMerger;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.RangeVectorResult;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.VectorData.Sample;
import org.okapi.promql.eval.VectorData.SeriesSample;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;

/** Evaluates histogram_quantile(q, vector-of-histograms). */
public final class HistogramQuantileEval implements Evaluable {
  private final FunctionExpr fn;

  public HistogramQuantileEval(FunctionExpr fn) {
    this.fn = fn;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 2)
      throw new IllegalArgumentException("histogram_quantile(q, range-vector)");

    // q must be scalar
    var qRes = fn.args.get(0).lower().eval(ctx);
    if (!(qRes instanceof org.okapi.promql.eval.ScalarResult s))
      throw new IllegalArgumentException("q must be scalar");
    double q = s.value;

    var vecRes = fn.args.get(1).lower().eval(ctx);
    if (!(vecRes instanceof RangeVectorResult rv))
      throw new IllegalArgumentException("histogram_quantile expects range vector of histograms");

    // Group by label set to merge histograms across series with identical labels
    List<SeriesSample> out = new ArrayList<>();
    var grouped = new LinkedHashMap<Map<String, String>, List<VectorData.SeriesWindow>>();
    for (VectorData.SeriesWindow w : rv.data()) {
      Map<String, String> key = new HashMap<>(w.id().labels().tags());
      key.remove("instance"); // merge across instances by default
      grouped
          .computeIfAbsent(java.util.Collections.unmodifiableMap(key), k -> new ArrayList<>())
          .add(w);
    }
    for (var e : grouped.entrySet()) {
      List<HistoScan> toMerge = new ArrayList<>();
      VectorData.SeriesId rep = null;
      for (var w : e.getValue()) {
        if (rep == null) {
          rep = new VectorData.SeriesId(w.id().metric(), new VectorData.Labels(e.getKey()));
        }
        Scan scan = w.scan();
        if (scan instanceof HistoScan hs) toMerge.add(hs);
      }
      if (toMerge.isEmpty()) continue;
      HistoScan merged = HistoScanMerger.merge("", toMerge);
      float value = quantileFromHistogram(q, merged);
      for (long t = ctx.startMs; t <= ctx.endMs; t += ctx.stepMs) {
        out.add(new SeriesSample(rep, new Sample(t, value)));
      }
    }
    return new InstantVectorResult(out);
  }

  // Compute quantile value from histogram (ubs, counts)
  static float quantileFromHistogram(double q, HistoScan hs) {
    List<Float> ubs = hs.getUbs();
    List<Integer> counts = hs.getCounts();
    if (counts == null || counts.isEmpty()) return Float.NaN;

    long total = 0;
    for (int c : counts) total += c;
    if (total <= 0) return Float.NaN;

    // Target rank using total, aligning with Prometheus-like interpolation
    double target = q * total;
    long cum = 0;
    int k = -1;
    for (int i = 0; i < counts.size(); i++) {
      long next = cum + counts.get(i);
      if (target <= next) {
        k = i;
        break;
      }
      cum = next;
    }
    if (k == -1) k = counts.size() - 1;

    int n = ubs.size(); // number of finite buckets
    float lower, upper;
    if (k == 0) {
      lower = Float.NEGATIVE_INFINITY;
      upper = ubs.get(0);
    } else if (k < n) {
      lower = ubs.get(k - 1);
      upper = ubs.get(k);
    } else { // k == n (the +inf bucket)
      lower = (n >= 1) ? ubs.get(n - 1) : Float.NEGATIVE_INFINITY;
      upper = Float.POSITIVE_INFINITY;
    }

    int inBucket = counts.get(k);
    if (inBucket <= 0) {
      // Degenerate bucket: return upper or lower bound if known
      if (Float.isInfinite(upper)) return lower;
      if (Float.isInfinite(lower)) return upper;
      return upper;
    }

    double pos = (target - cum) / Math.max(inBucket, 1);
    if (Float.isInfinite(lower)) return upper;
    if (Float.isInfinite(upper)) return lower;
    return (float) (lower + pos * (upper - lower));
  }
}

// eval/ops/SubqueryEval.java
package org.okapi.promql.eval.ops;

import java.util.*;
import org.okapi.Statistics;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.SubqueryExpr;

/**
 * Evaluates inner expr over (t-range, t] with subquery step; wraps inner samples as single-sample
 * Statistics.
 */
public final class SubqueryEval implements Evaluable {
  private final SubqueryExpr node;

  public SubqueryEval(SubqueryExpr node) {
    this.node = node;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    long outerStart = ctx.startMs;
    long outerEnd = ctx.endMs;
    long outerStep = ctx.stepMs;
    long offset = node.offsetMs == null ? 0L : node.offsetMs;

    List<SeriesWindow> out = new ArrayList<>();

    // Evaluate inner at each sub-step across the union range [outerStart - range, outerEnd]
    long subStart = outerStart - node.rangeMs - offset;
    long subEnd = outerEnd - offset;

    var subCtx =
        new EvalContext(
            subStart, subEnd, node.stepMs, ctx.resolution, ctx.client, ctx.discovery, ctx.exec);
    var innerRes = node.inner.lower().eval(subCtx);

    // We need (series → list of (substep ts, value as Statistics))
    if (innerRes instanceof InstantVectorResult iv) {
      // regroup by series
      Map<SeriesId, List<StatsPoint>> map = new LinkedHashMap<>();
      for (SeriesSample s : iv.data()) {
        map.computeIfAbsent(s.series(), k -> new ArrayList<>())
            .add(new StatsPoint(s.sample().ts(), new SingleSampleStatistics(s.sample().value())));
      }
      // keep order
      for (var e : map.entrySet()) {
        e.getValue().sort(Comparator.comparingLong(StatsPoint::ts));
        out.add(new SeriesWindow(e.getKey(), e.getValue()));
      }
      return new RangeVectorResult(out);
    } else if (innerRes instanceof RangeVectorResult rv) {
      // Already a range vector: pass through, but apply offset by shifting timestamps if needed
      if (offset == 0) return rv;
      List<SeriesWindow> shifted = new ArrayList<>(rv.data().size());
      for (SeriesWindow w : rv.data()) {
        List<StatsPoint> pts = new ArrayList<>(w.points().size());
        for (var p : w.points()) pts.add(new StatsPoint(p.ts() + offset, p.stats()));
        shifted.add(new SeriesWindow(w.id(), pts));
      }
      return new RangeVectorResult(shifted);
    }

    return innerRes; // scalar: nothing to do
  }

  /** Minimal single-sample Statistics adapter for subquery samples. */
  static final class SingleSampleStatistics implements Statistics {
    private final float v;

    SingleSampleStatistics(float v) {
      this.v = v;
    }

    @Override
    public float percentile(double q) {
      return v;
    }

    @Override
    public float avg() {
      return v;
    }

    @Override
    public float min() {
      return v;
    }

    @Override
    public float max() {
      return v;
    }

    @Override
    public byte[] serialize() {
      throw new UnsupportedOperationException();
    }

    @Override
    public float getSum() {
      return v;
    }

    @Override
    public float getCount() {
      return 1f;
    }

    @Override
    public float getSumOfDeviationsSquared() {
      return 0;
    }
  }
}

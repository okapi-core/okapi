// eval/ops/SubqueryEval.java
package org.okapi.promql.eval.ops;

import java.util.*;
import org.okapi.metrics.pojos.results.GaugeScan;
import org.okapi.promql.eval.*;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.SubqueryExpr;

/**
 * Evaluates inner expr over (t-range, t] with subquery step; wraps inner samples into GaugeScans
 * per series for downstream range processing.
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
    long offset = node.offsetMs == null ? 0L : node.offsetMs;

    List<SeriesWindow> out = new ArrayList<>();

    // Evaluate inner at each sub-step across the union range [outerStart - range, outerEnd]
    long subStart = outerStart - node.rangeMs - offset;
    long subEnd = outerEnd - offset;

    var subCtx =
        new EvalContext(
            subStart, subEnd, node.stepMs, ctx.resolution, ctx.client, ctx.discovery, ctx.exec);
    var innerRes = node.inner.lower().eval(subCtx);

    // We need (series → GaugeScan of (substep ts, value))
    if (innerRes instanceof InstantVectorResult iv) {
      // regroup by series and build GaugeScan per series
      Map<SeriesId, List<Sample>> map = new LinkedHashMap<>();
      for (SeriesSample s : iv.data()) {
        map.computeIfAbsent(s.series(), k -> new ArrayList<>()).add(s.sample());
      }
      for (var e : map.entrySet()) {
        var samples = e.getValue();
        samples.sort(Comparator.comparingLong(Sample::ts));
        List<Long> ts = new ArrayList<>(samples.size());
        List<Float> vals = new ArrayList<>(samples.size());
        for (var smp : samples) {
          ts.add(smp.ts());
          vals.add(smp.value());
        }
        GaugeScan gs =
            GaugeScan.builder()
                .universalPath("")
                .timestamps(Collections.unmodifiableList(ts))
                .values(Collections.unmodifiableList(vals))
                .build();
        out.add(new SeriesWindow(e.getKey(), gs));
      }
      return new RangeVectorResult(out);
    } else if (innerRes instanceof RangeVectorResult rv) {
      // Already a range vector: pass through, but apply offset by shifting timestamps if needed
      if (offset == 0) return rv;
      List<SeriesWindow> shifted = new ArrayList<>(rv.data().size());
      for (SeriesWindow w : rv.data()) {
        var scan = w.scan();
        if (scan instanceof GaugeScan gs) {
          List<Long> ts = new ArrayList<>(gs.getTimestamps().size());
          for (Long t : gs.getTimestamps()) ts.add(t + offset);
          GaugeScan shiftedGs =
              GaugeScan.builder()
                  .universalPath(gs.getUniversalPath())
                  .timestamps(Collections.unmodifiableList(ts))
                  .values(gs.getValues())
                  .build();
          shifted.add(new SeriesWindow(w.id(), shiftedGs));
        } else {
          // For other scan types, leave as-is (or could handle shifting if needed)
          shifted.add(w);
        }
      }
      return new RangeVectorResult(shifted);
    }

    return innerRes; // scalar: nothing to do
  }
}

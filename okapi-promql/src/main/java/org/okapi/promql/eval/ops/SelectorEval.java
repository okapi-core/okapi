package org.okapi.promql.eval.ops;

// eval/ops/SelectorEval.java
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.RangeVectorResult;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.nodes.SelectorExpr;

@AllArgsConstructor
public class SelectorEval implements Evaluable {
  private final SelectorExpr node;

  @Override
  public ExpressionResult eval(EvalContext ctx) {
    long start = ctx.startMs, end = ctx.endMs;
    if (node.atTsMs != null) {
      start = end = node.atTsMs;
    }
    if (node.offsetMs != null) {
      start -= node.offsetMs;
      end -= node.offsetMs;
    }

    var series = ctx.discovery.expand(node.metricOrNull, node.matchers, start, end);
    // For an instant-vector at each step, we need the last point at each timestamp (Prom
    // semantics).
    // Here we fetch a window covering [start, end], then at materialization time we select
    // per-step.
    List<SeriesWindow> windows = new ArrayList<>(series.size());
    for (SeriesId id : series) {
      Map<Long, Statistics> raw =
          ctx.client.get(id.metric(), id.labels().tags(), ctx.resolution, start, end);
      var points =
          raw.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .map(e -> new StatsPoint(e.getKey(), e.getValue()))
              .toList();
      windows.add(new SeriesWindow(id, points));
    }
    return new RangeVectorResult(
        windows); // callers (e.g. instantizer or range fns) will consume appropriately
  }
}

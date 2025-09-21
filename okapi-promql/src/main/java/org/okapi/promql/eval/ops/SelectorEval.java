package org.okapi.promql.eval.ops;

// eval/ops/SelectorEval.java
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.pojos.results.Scan;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.RangeVectorResult;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.nodes.SelectorExpr;

@Slf4j
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
    List<SeriesWindow> windows = new ArrayList<>(series.size());
    for (SeriesId id : series) {
      Scan scan = ctx.client.get(id.metric(), id.labels().tags(), ctx.resolution, start, end);
      windows.add(new SeriesWindow(id, scan));
    }
    return new RangeVectorResult(windows);
  }
}

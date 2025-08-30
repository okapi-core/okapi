package org.okapi.promql.eval.ops;

import lombok.AllArgsConstructor;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.nodes.RangeSelectorExpr;
import org.okapi.promql.eval.nodes.SelectorExpr;

@AllArgsConstructor
public class RangeSelectorEval implements Evaluable {
  private final RangeSelectorExpr node;

  @Override
  public ExpressionResult eval(EvalContext ctx) {
    // Evaluate as the underlying selector with expanded time to cover [start - range, end]
    long start = ctx.startMs - node.rangeMs;
    long end = ctx.endMs;
    if (node.offsetMs != null) {
      start -= node.offsetMs;
      end -= node.offsetMs;
    }

    var baseSel =
        new SelectorExpr(node.base.metricOrNull, node.base.matchers, node.base.atTsMs, null);
    var baseRes =
        new SelectorEval(baseSel)
            .eval(
                new EvalContext(
                    start, end, ctx.stepMs, ctx.resolution, ctx.client, ctx.discovery, ctx.exec));
    return baseRes; // range fns will slide windows of length rangeMs per step
  }
}

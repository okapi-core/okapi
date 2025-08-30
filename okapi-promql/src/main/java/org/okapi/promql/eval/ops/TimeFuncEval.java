package org.okapi.promql.eval.ops;

import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.ScalarResult;
import org.okapi.promql.eval.nodes.FunctionExpr;

public class TimeFuncEval implements Evaluable {
  private final FunctionExpr fn;

  TimeFuncEval(FunctionExpr fn) {
    this.fn = fn;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) {
    // Return current evaluation time (end) in seconds
    return new ScalarResult(ctx.endMs / 1000f);
  }
}

package org.okapi.promql.eval.ops;

import org.okapi.promql.eval.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;

public class ScalarFuncEval implements Evaluable {
  private final FunctionExpr fn;

  ScalarFuncEval(FunctionExpr fn) {
    this.fn = fn;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 1) throw new IllegalArgumentException("scalar(vector) expects one arg");
    var r = fn.args.get(0).lower().eval(ctx);
    if (r instanceof ScalarResult s) return s;
    if (r instanceof InstantVectorResult iv) {
      if (iv.data().size() != 1) return new ScalarResult(Float.NaN);
      return new ScalarResult(iv.data().get(0).sample().value());
    }
    return new ScalarResult(Float.NaN);
  }
}

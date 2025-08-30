package org.okapi.promql.eval.ops;

import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.ScalarResult;
import org.okapi.promql.eval.exceptions.EvaluationException;

public final class AtEval implements Evaluable {
  private final Evaluable inner;
  private final Evaluable atScalar;

  public AtEval(Evaluable inner, Evaluable atScalar) {
    this.inner = inner;
    this.atScalar = atScalar;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    var s = atScalar.eval(ctx);
    if (!(s instanceof ScalarResult sr)) {
      throw new EvaluationException("@ modifier requires scalar RHS (unix seconds)");
    }
    long tsMs = (long) (sr.value * 1000L);
    var pinned =
        new EvalContext(
            tsMs, tsMs, ctx.stepMs, ctx.resolution, ctx.client, ctx.discovery, ctx.exec);
    return inner.eval(pinned);
  }
}

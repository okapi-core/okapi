package org.okapi.promql.eval.ops;

import lombok.AllArgsConstructor;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.exceptions.EvaluationException;

// eval/ops/OffsetEval.java
@AllArgsConstructor
public class OffsetEval implements Evaluable {
  private final Evaluable inner;
  private final long offsetMs;

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    var shifted =
        new EvalContext(
            ctx.startMs - offsetMs,
            ctx.endMs - offsetMs,
            ctx.stepMs,
            ctx.resolution,
            ctx.client,
            ctx.discovery,
            ctx.exec);
    return inner.eval(shifted);
  }
}

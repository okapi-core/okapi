/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.ops;

import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.VectorData.*;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;

public class AbsentEval implements Evaluable {
  private final FunctionExpr fn;

  AbsentEval(FunctionExpr fn) {
    this.fn = fn;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 1) throw new IllegalArgumentException("absent(expr) expects one arg");
    var r = fn.args.get(0).lower().eval(ctx);
    if (r instanceof InstantVectorResult iv) {
      if (iv.data().isEmpty())
        return new InstantVectorResult(
            java.util.List.of(
                new SeriesSample(
                    new SeriesId("absent", new Labels(java.util.Map.of())),
                    new Sample(ctx.endMs, 1f))));
      return new InstantVectorResult(java.util.List.of());
    }
    return new InstantVectorResult(java.util.List.of());
  }
}

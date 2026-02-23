/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.ops;

import java.util.ArrayList;
import org.okapi.promql.eval.EvalContext;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.ExpressionResult;
import org.okapi.promql.eval.InstantVectorResult;
import org.okapi.promql.eval.exceptions.EvaluationException;
import org.okapi.promql.eval.nodes.FunctionExpr;

public class SortEval implements Evaluable {
  private final FunctionExpr fn;
  private final boolean desc;

  SortEval(FunctionExpr fn, boolean desc) {
    this.fn = fn;
    this.desc = desc;
  }

  @Override
  public ExpressionResult eval(EvalContext ctx) throws EvaluationException {
    if (fn.args.size() != 1) throw new IllegalArgumentException("sort(expr) expects one arg");
    var r = fn.args.get(0).lower().eval(ctx);
    if (!(r instanceof InstantVectorResult iv))
      throw new IllegalArgumentException("sort expects instant vector");
    var data = new ArrayList<>(iv.data());
    data.sort((a, b) -> Float.compare(a.sample().value(), b.sample().value()));
    if (desc) java.util.Collections.reverse(data);
    return new InstantVectorResult(data);
  }
}

package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.AtEval;

public final class AtExpr implements LogicalExpr {
  public final LogicalExpr inner;
  public final LogicalExpr atScalar; // expected to evaluate to scalar seconds

  public AtExpr(LogicalExpr inner, LogicalExpr atScalar) {
    this.inner = inner;
    this.atScalar = atScalar;
  }

  @Override
  public Evaluable lower() {
    return new AtEval(inner.lower(), atScalar.lower());
  }
}

package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.SubqueryEval;

public final class SubqueryExpr implements LogicalExpr {
  public final LogicalExpr inner;
  public final long rangeMs;
  public final long stepMs;
  public final Long offsetMs; // nullable

  public SubqueryExpr(LogicalExpr inner, long rangeMs, long stepMs, Long offsetMs) {
    this.inner = inner;
    this.rangeMs = rangeMs;
    this.stepMs = stepMs;
    this.offsetMs = offsetMs;
  }

  @Override
  public Evaluable lower() {
    return new SubqueryEval(this);
  }
}

package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.OffsetEval;

public final class OffsetExpr implements LogicalExpr {
    public final LogicalExpr inner;
    public final long offsetMs;
    public OffsetExpr(LogicalExpr inner, long offsetMs) { this.inner = inner; this.offsetMs = offsetMs; }
    @Override public Evaluable lower() { return new OffsetEval(inner.lower(), offsetMs); }
}

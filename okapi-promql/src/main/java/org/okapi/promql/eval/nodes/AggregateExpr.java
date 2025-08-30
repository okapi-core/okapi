package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.AggregateEval;

import java.util.List;

public final class AggregateExpr implements LogicalExpr {
    public final String op;                    // sum, avg, min, max, count, quantile, ...
    public final List<String> groupLabels;     // by(...) or without(...)
    public final boolean isBy;                 // true=by, false=without; if neither, groupLabels=[]
    public final List<LogicalExpr> args;       // usually 1, quantile has 2 (q, vector)

    public AggregateExpr(String op, boolean isBy, List<String> groupLabels, List<LogicalExpr> args) {
        this.op = op; this.isBy = isBy; this.groupLabels = groupLabels; this.args = args;
    }
    @Override public Evaluable lower() { return new AggregateEval(this); }
}

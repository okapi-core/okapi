package org.okapi.promql.eval.nodes;

// eval/nodes/FunctionExpr.java
import lombok.AllArgsConstructor;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.FunctionRegistry;

import java.util.*;

@AllArgsConstructor
public final class FunctionExpr implements LogicalExpr {
    public final String name;
    public final List<LogicalExpr> args;
    public final FunctionRegistry functionRegistry;
    @Override public Evaluable lower() { return this.functionRegistry.lower(this); }
}


package org.okapi.promql.eval.nodes;

// eval/nodes/BinaryOpExpr.java
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.BinaryOpEval;

import java.util.*;

public final class BinaryOpExpr implements LogicalExpr {
    public final String op; // + - * / % ^, and, or, unless, compare ops with bool?
    public final LogicalExpr left, right;
    public final MatchSpec matchSpec; // on/ignoring + group_left/right (may be null)
    public final boolean boolModifier; // for comparisons like > bool

    public BinaryOpExpr(String op, LogicalExpr left, LogicalExpr right,
                        MatchSpec matchSpec, boolean boolModifier) {
        this.op = op; this.left = left; this.right = right;
        this.matchSpec = matchSpec; this.boolModifier = boolModifier;
    }
    @Override public Evaluable lower() { return new BinaryOpEval(this); }
}

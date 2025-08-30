package org.okapi.promql.eval.nodes;


import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.SelectorEval;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

public final class SelectorExpr implements LogicalExpr {
    public final String metricOrNull;          // may be null for {}-only
    public final List<LabelMatcher> matchers;  // may be empty
    public final Long atTsMs;                  // null unless set by @ modifier
    public final Long offsetMs;                // null unless offset applied

    public SelectorExpr(String metricOrNull, List<LabelMatcher> matchers, Long atTsMs, Long offsetMs) {
        this.metricOrNull = metricOrNull; this.matchers = matchers;
        this.atTsMs = atTsMs; this.offsetMs = offsetMs;
    }

    @Override public Evaluable lower() { return new SelectorEval(this); }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.RangeSelectorEval;

// eval/nodes/RangeSelectorExpr.java
public final class RangeSelectorExpr implements LogicalExpr {
  public final SelectorExpr base;
  public final long rangeMs; // e.g. [5m]
  public final Long offsetMs; // optional

  public RangeSelectorExpr(SelectorExpr base, long rangeMs, Long offsetMs) {
    this.base = base;
    this.rangeMs = rangeMs;
    this.offsetMs = offsetMs;
  }

  @Override
  public Evaluable lower() {
    return new RangeSelectorEval(this);
  }
}

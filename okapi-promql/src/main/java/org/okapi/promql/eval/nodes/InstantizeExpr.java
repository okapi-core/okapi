/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.InstantizeEval;

public final class InstantizeExpr implements LogicalExpr {
  public final LogicalExpr inner;

  public InstantizeExpr(LogicalExpr inner) {
    this.inner = inner;
  }

  @Override
  public Evaluable lower() {
    return new InstantizeEval(inner.lower());
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.nodes;

import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ScalarResult;

// eval/nodes/LiteralExpr.java
public final class LiteralExpr implements LogicalExpr {
  private final float value;

  public LiteralExpr(float value) {
    this.value = value;
  }

  @Override
  public Evaluable lower() {
    return ctx -> new ScalarResult(value);
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.nodes;

// eval/nodes/FunctionExpr.java
import java.util.*;
import lombok.AllArgsConstructor;
import org.okapi.promql.eval.Evaluable;
import org.okapi.promql.eval.LogicalExpr;
import org.okapi.promql.eval.ops.FunctionRegistry;

@AllArgsConstructor
public final class FunctionExpr implements LogicalExpr {
  public final String name;
  public final List<LogicalExpr> args;
  public final FunctionRegistry functionRegistry;

  @Override
  public Evaluable lower() {
    return this.functionRegistry.lower(this);
  }
}

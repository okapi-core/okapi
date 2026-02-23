/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval;

// eval/LogicalExpr.java
public interface LogicalExpr {
  Evaluable lower();
}

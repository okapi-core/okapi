/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval;

import org.okapi.promql.eval.exceptions.EvaluationException;

public interface Evaluable {
  ExpressionResult eval(EvalContext ctx) throws EvaluationException;
}

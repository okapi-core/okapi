package org.okapi.promql.eval;

import org.okapi.promql.eval.exceptions.EvaluationException;

// eval/Evaluable.java
public interface Evaluable {
  ExpressionResult eval(EvalContext ctx) throws EvaluationException;
}

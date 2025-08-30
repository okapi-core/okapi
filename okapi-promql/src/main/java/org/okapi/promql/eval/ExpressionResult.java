package org.okapi.promql.eval;

public sealed interface ExpressionResult
    permits ScalarResult, InstantVectorResult, RangeVectorResult {
  ValueType type();
}

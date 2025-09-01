package org.okapi.promql.eval;

import lombok.Getter;

// eval/ScalarResult.java
public final class ScalarResult implements ExpressionResult {
  @Getter public final float value;

  public ScalarResult(float value) {
    this.value = value;
  }

  @Override
  public ValueType type() {
    return ValueType.SCALAR;
  }
}

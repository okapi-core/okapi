package org.okapi.promql.eval;

// eval/ScalarResult.java
public final class ScalarResult implements ExpressionResult {
    public final float value;
    public ScalarResult(float value) { this.value = value; }
    @Override public ValueType type() { return ValueType.SCALAR; }
}

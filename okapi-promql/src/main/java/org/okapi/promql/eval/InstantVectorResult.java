package org.okapi.promql.eval;

import org.okapi.promql.eval.VectorData.SeriesSample;

import java.util.Iterator;
import java.util.List;

public final class InstantVectorResult implements ExpressionResult, Iterable<SeriesSample> {
  private final List<SeriesSample> data;

  public InstantVectorResult(List<SeriesSample> data) {
    this.data = data;
  }

  public List<SeriesSample> data() {
    return data;
  }

  @Override
  public Iterator<SeriesSample> iterator() {
    return data.iterator();
  }

  @Override
  public ValueType type() {
    return ValueType.INSTANT_VECTOR;
  }
}

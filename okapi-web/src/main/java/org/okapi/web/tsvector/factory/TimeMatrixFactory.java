package org.okapi.web.tsvector.factory;

import org.okapi.web.tsvector.LegendFn;
import org.okapi.web.tsvector.TimeMatrix;

public interface TimeMatrixFactory<LegendIn, T> {
  TimeMatrix createTimeMatrix(LegendFn<LegendIn> legendFn, T base);
}

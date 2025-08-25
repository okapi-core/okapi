package org.okapi.metrics.stats;

import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;

public interface Statistics {
  void update(MetricsContext ctx, float f) throws StatisticsFrozenException;

  void update(MetricsContext ctx, float[] arr) throws StatisticsFrozenException;

  float percentile(double quantile);

  float avg();

  float min();

  float max();

  byte[] serialize();

  float aggregate(AGG_TYPE aggType);

  float getSum();

  float getCount();

  float getSumOfDeviationsSquared();

  boolean freeze();

  long bufferSize();
}

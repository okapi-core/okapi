package org.okapi.metrics.stats;

import org.apache.datasketches.quantilescommon.QuantilesFloatsAPI;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;

public interface Statistics {
  static RolledUpStatistics deserialize(byte[] bytes, QuantileRestorer restorer){throw new IllegalArgumentException();};

  void update(MetricsContext ctx, float f);

  void update(MetricsContext ctx, float[] arr);

  float percentile(double quantile);

  float avg();

  float min();

  float max();

  byte[] serialize();

  float aggregate(AGG_TYPE aggType);

  float getSum();

  float getCount();

  float getSumOfDeviationsSquared();

  QuantilesFloatsAPI getFloatsQuantiles();
}

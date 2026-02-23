/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import org.okapi.Statistics;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.pojos.AGG_TYPE;

public interface UpdatableStatistics extends Statistics {
  void update(MetricsContext ctx, float f) throws StatisticsFrozenException;

  void update(MetricsContext ctx, float[] arr) throws StatisticsFrozenException;

  byte[] serialize();

  float aggregate(AGG_TYPE aggType);

  boolean freeze();
}

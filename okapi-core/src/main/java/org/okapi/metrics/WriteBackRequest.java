/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

import lombok.Value;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.stats.UpdatableStatistics;

@Value
public class WriteBackRequest {
  MetricsContext context;
  int shard;
  String key;
  UpdatableStatistics statistics;
}

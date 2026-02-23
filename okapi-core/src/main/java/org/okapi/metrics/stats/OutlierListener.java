/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import org.okapi.metrics.common.MetricsContext;

public interface OutlierListener {
  void notify(MetricsContext metricsContext, float f);
}

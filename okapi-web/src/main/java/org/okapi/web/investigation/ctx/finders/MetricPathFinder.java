/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx.finders;

import java.util.List;

public interface MetricPathFinder {
  public List<MetricPath> findRelateMetrics(String dependencyName);
}

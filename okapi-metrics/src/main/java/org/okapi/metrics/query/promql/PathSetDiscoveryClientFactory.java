/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query.promql;

import lombok.AllArgsConstructor;
import org.okapi.metrics.query.PathSet;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.runtime.SeriesDiscoveryFactory;

@AllArgsConstructor
public class PathSetDiscoveryClientFactory implements SeriesDiscoveryFactory {
  PathSet pathSet;

  @Override
  public SeriesDiscovery get(String tenantId) {
    return new PathSetSeriesDiscovery(tenantId, pathSet);
  }
}

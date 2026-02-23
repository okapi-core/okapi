/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.rollup;

import java.util.List;
import org.okapi.metrics.common.MetricsPathParser;

public interface TsSearcher {
  List<SearchResult> search(String tenantId, String pattern, long start, long end);

  List<MetricsPathParser.MetricsRecord> list(String tenantId, long start, long end);
}

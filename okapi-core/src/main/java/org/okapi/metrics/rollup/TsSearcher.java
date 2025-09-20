package org.okapi.metrics.rollup;

import org.okapi.metrics.common.MetricsPathParser;

import java.util.List;

public interface TsSearcher {
  List<SearchResult> search(String tenantId,  String pattern, long start, long end);
  List<MetricsPathParser.MetricsRecord> list(String tenantId, long start, long end);
}

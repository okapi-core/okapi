package org.okapi.metrics.fdb;

import lombok.AllArgsConstructor;
import org.okapi.metrics.query.promql.PromQlPathMatcher;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

import java.util.List;

@AllArgsConstructor
public class FdbSeriesDiscovery implements SeriesDiscovery {
  String tenantId;

  TsSearcher searcher;

  @Override
  public List<VectorData.SeriesId> expand(
      String metricOrNull, List<LabelMatcher> matchers, long start, long end) {
    var candidates =
        searcher.list(tenantId, start, end).stream()
            .filter(
                c -> {
                  if (metricOrNull == null) return true;
                  else return c.name().equals(metricOrNull);
                })
            .filter(record -> PromQlPathMatcher.pathMatchesConditions(record, matchers))
            .map(
                record ->
                    new VectorData.SeriesId(record.name(), new VectorData.Labels(record.tags())))
            .toList();
    return candidates;
  }
}

package org.okapi.metrics.query.promql;

import static org.okapi.metrics.query.promql.PromQlPathMatcher.pathMatchesConditions;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.paths.PathSet;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

@AllArgsConstructor
public class PathSetSeriesDiscovery implements SeriesDiscovery {

  String tenantId;
  PathSet pathSet;

  @Override
  public List<VectorData.SeriesId> expand(String metricOrNull, List<LabelMatcher> matchers, long st, long en) {
    var allMetrics = pathSet.list();
    var matches = new ArrayList<VectorData.SeriesId>();
    for (var entry : allMetrics.entrySet()) {
      var allPaths = entry.getValue();
      for (var path : allPaths) {
        var maybeParsed = MetricsPathParser.parse(path);
        if (maybeParsed.isEmpty()) continue;
        var parsed = maybeParsed.get();
        if (!parsed.tenantId().equals(tenantId)) continue;
        // matchers are always AND-ed
        // if a label doesn't exist -> no match
        // if label exists -> OP(label_value, patternOrValue)
        var matchesConditions = pathMatchesConditions(parsed, matchers);
        if (matchesConditions) {
          matches.add(new VectorData.SeriesId(parsed.name(), new VectorData.Labels(parsed.tags())));
        }
      }
    }
    return matches;
  }

}

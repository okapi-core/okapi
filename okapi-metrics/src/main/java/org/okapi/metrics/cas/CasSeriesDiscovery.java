package org.okapi.metrics.cas;

import com.google.re2j.Pattern;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.rollup.SearchResult;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.promql.eval.VectorData;
import org.okapi.promql.eval.ts.SeriesDiscovery;
import org.okapi.promql.parse.LabelMatcher;

@Slf4j
@AllArgsConstructor
public class CasSeriesDiscovery implements SeriesDiscovery {
  String tenantId;
  TsSearcher searcher;

  @Override
  public List<VectorData.SeriesId> expand(
      String metricOrNull, List<LabelMatcher> matchers, long start, long end) {
    String metricPattern = (metricOrNull == null || metricOrNull.isBlank()) ? "*" : metricOrNull;
    List<SearchResult> candidates = searcher.search(tenantId, metricPattern, start, end);
    log.info("Found {} candidates", candidates);

    List<VectorData.SeriesId> out = new ArrayList<>();
    for (SearchResult sr : candidates) {
      // Extra safety: enforce metric name if provided
      if (metricOrNull != null && !metricOrNull.equals(sr.getName())) continue;
      if (matchesAll(sr.getName(), sr.getTags(), matchers)) {
        out.add(new VectorData.SeriesId(sr.getName(), new VectorData.Labels(sr.getTags())));
      }
    }
    return out;
  }

  private static boolean matchesAll(
      String metricName, Map<String, String> labels, List<LabelMatcher> matchers) {
    if (matchers == null || matchers.isEmpty()) return true;
    for (LabelMatcher m : matchers) {
      String actual = m.name().equals("__name__") ? metricName : labels.get(m.name());
      switch (m.op()) {
        case EQ -> {
          if (actual == null || !actual.equals(m.value())) return false;
        }
        case NE -> {
          if (actual != null && actual.equals(m.value())) return false;
        }
        case RE -> {
          if (actual == null) return false;
          if (!Pattern.compile(m.value()).matches(actual)) return false;
        }
        case NRE -> {
          if (actual != null && Pattern.compile(m.value()).matches(actual)) return false;
        }
      }
    }
    return true;
  }
}

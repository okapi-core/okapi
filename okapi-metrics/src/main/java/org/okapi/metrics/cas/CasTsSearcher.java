package org.okapi.metrics.cas;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.okapi.metrics.cas.dao.SearchHintDao;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.rollup.SearchResult;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.search.MetricsSearcher;

@Slf4j
@AllArgsConstructor
public class CasTsSearcher implements TsSearcher {

  SearchHintDao searchHintDao;

  public static final Duration ONE_MIN = Duration.of(1, ChronoUnit.MINUTES);

  @Override
  public List<SearchResult> search(String tenantId, String pattern, long start, long end) {
    // ignore shard key for now -> to be used should be number of metrics exceed a couple of
    // millions
    return searchPattern(tenantId, pattern, start, end).stream().toList();
  }

  private Collection<SearchResult> searchPattern(
      String tenantId, String pattern, long start, long end) {
    var bs = start / ONE_MIN.toMillis();
    var bEn = end / ONE_MIN.toMillis();
    var hints = new HashSet<SearchResult>();
    var patternInfo = MetricsSearcher.PatternInfo.parse(pattern);
    var matched = new ArrayList<String>();
    searchHintDao
        .scan(tenantId, 0, bs, bEn)
        .iterator()
        .forEachRemaining(
            hint -> {
              var univPath = MetricPaths.getUnivPath(tenantId, hint.getLocalPath());
              var isAMatch = MetricsSearcher.isAMatch(tenantId, univPath, patternInfo);
              if (!isAMatch) {
                return;
              } else {
                var parsed = MetricsPathParser.parse(univPath);
                if (parsed.isEmpty()) return;
                if (matched.contains(univPath)) return;
                matched.add(univPath);
                hints.add(
                    SearchResult.builder()
                        .tenantId(tenantId)
                        .name(parsed.get().name())
                        .tags(parsed.get().tags())
                        .type(hint.getMetricType())
                        .build());
              }
            });
    return hints;
  }

  @Override
  public List<MetricsPathParser.MetricsRecord> list(String tenantId, long start, long end) {
    throw new NotImplementedException();
  }
}

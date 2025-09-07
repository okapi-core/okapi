package org.okapi.metrics.fdb;

import com.apple.foundationdb.Database;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.okapi.metrics.common.MetricsPathParser;
import org.okapi.metrics.fdb.tuples.SearchTuple;
import org.okapi.metrics.rollup.SearchResult;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.search.MetricsSearcher;

@AllArgsConstructor
public class FdbTsSearcher implements TsSearcher {

  Database database;

  @Override
  public List<SearchResult> search(String tenantId, String pattern, long start, long end) {
    var candidates = listPaths(tenantId, start, end);
    var searcher = MetricsSearcher.searchMatchingMetrics(tenantId, candidates, pattern);
    return searcher.stream()
        .map(record -> new SearchResult(record.tenantId(), record.name(), record.tags()))
        .toList();
  }

  @Override
  public List<MetricsPathParser.MetricsRecord> list(String tenantId, long start, long end) {
    var candidates = listPaths(tenantId, start, end);
    return candidates.stream()
        .map(MetricsPathParser::parse)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  private Collection<String> listPaths(String tenantId, long start, long end) {
    var blockStart = start / 60_000;
    var blockEnd = end / 60_000;
    var searchRange = SearchTuple.searchRange(tenantId, blockStart, blockEnd);
    return database
        .run(
            tr -> {
              return tr.read(
                  read -> {
                    var range = read.getRange(searchRange[0], searchRange[1]);
                    var iter = range.iterator();
                    var list = Lists.newArrayList(iter);
                    return list;
                  });
            })
        .stream()
        .map(v -> SearchTuple.fromKey(v.getKey()))
        .map(
            searchTuple -> {
              var local = searchTuple.getLocalPath();
              var tenant = searchTuple.getTenant();
              var universalPath = tenant + ":" + local;
              return universalPath;
            })
        .collect(Collectors.toSet());
  }
}

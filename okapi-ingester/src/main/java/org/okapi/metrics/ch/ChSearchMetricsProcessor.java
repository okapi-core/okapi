package org.okapi.metrics.ch;

import com.clickhouse.client.api.Client;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.okapi.ch.ChTemplateFiles;
import org.okapi.metrics.ch.rows.ChSearchMetricsRow;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.metrics.ch.template.ChSearchMetricsQueryTemplate;
import org.okapi.rest.search.MetricPath;
import org.okapi.rest.search.SearchMetricsRequest;
import org.okapi.rest.search.SearchMetricsResponse;

@AllArgsConstructor
public class ChSearchMetricsProcessor {
  private final Client client;
  private final ChMetricTemplateEngine templateEngine;

  public SearchMetricsResponse searchMetricsResponse(SearchMetricsRequest request) {
    var matcher = MetricPathMatcher.fromRequest(request);
    var matchingPaths =
        getMetricsInTimeWindow(request.getTsStartMillis(), request.getTsEndMillis());
    var matching =
        matchingPaths.stream()
            .filter(row -> matcher.matches(row.getName(), row.getTags()))
            .map(this::fromPathToRow)
            .toList();
    return SearchMetricsResponse.builder().matchingPaths(matching).build();
  }

  public MetricPath fromPathToRow(ChSearchMetricsRow row) {
    return MetricPath.builder().metric(row.getName()).labels(row.getTags()).build();
  }

  public Set<ChSearchMetricsRow> getMetricsInTimeWindow(long tsStartMillis, long tsEndMillis) {
    var template =
        ChSearchMetricsQueryTemplate.builder()
            .table(ChConstants.TBL_METRIC_EVENTS_META)
            .startMs(tsStartMillis)
            .endMs(tsEndMillis)
            .build();
    var query = templateEngine.render(ChTemplateFiles.GET_METRIC_PATHS_IN_RANGE, template);
    var records = client.queryAll(query);
    var rows = new HashSet<ChSearchMetricsRow>();
    for (var record : records) {
      @SuppressWarnings("unchecked")
      var tags = (Map<String, String>) record.getObject("tags");
      rows.add(ChSearchMetricsRow.builder().name(record.getString("metric")).tags(tags).build());
    }
    return rows;
  }
}

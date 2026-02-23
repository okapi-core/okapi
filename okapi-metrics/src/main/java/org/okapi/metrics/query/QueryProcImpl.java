package org.okapi.metrics.query;

import lombok.AllArgsConstructor;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.rest.metrics.*;
import org.okapi.rest.metrics.query.*;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;

@AllArgsConstructor
public class QueryProcImpl implements QueryProcessor {
  TsReader tsReader;
  TsSearcher tsSearcher;

  @Override
  public GetMetricsResponse getMetricsResponse(GetMetricsRequest request) {
    var start = request.getStart();
    var end = request.getEnd();
    var res = request.getGaugeQueryConfig().getResolution();
    var path = MetricPaths.convertToUnivPath(request);
    var agg = request.getGaugeQueryConfig().getAggregation();
    var scanResult = tsReader.scanGauge(path, start, end, agg, res);
    return GetMetricsResponse.builder()
        .metric(request.getMetric())
        .tags(request.getTags())
        .gaugeResponse(
            GetGaugeResponse.builder()
                .resolution(res)
                .aggregation(agg)
                .times(scanResult.getTimestamps())
                .values(scanResult.getValues())
                .build())
        .build();
  }

  @Override
  public SearchMetricsResponse searchMetricsResponse(
      SearchMetricsRequestInternal searchMetricsRequest) {
    // list all metrics under the inverted path
    var start = searchMetricsRequest.getStartTime();
    var end = searchMetricsRequest.getEndTime();
    var pattern = searchMetricsRequest.getPattern();
    var tenant = searchMetricsRequest.getTenantId();
    var results = tsSearcher.search(tenant, pattern, start, end);
    return SearchMetricsResponse.builder()
        .results(
            results.stream()
                .map(
                    r -> MetricsPathSpecifier.builder().name(r.getName()).tags(r.getTags()).build())
                .toList())
        .build();
  }

  @Override
  public ListMetricsResponse listMetricsResponse(ListMetricsRequest request) {
    return ListMetricsResponse.builder()
        .results(
            tsSearcher
                .search(request.getTenantId(), "*", request.getStart(), request.getEnd())
                .stream()
                .map(
                    r -> MetricsPathSpecifier.builder().name(r.getName()).tags(r.getTags()).build())
                .toList())
        .build();
  }
}

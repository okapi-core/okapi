package org.okapi.metrics.query;

import lombok.AllArgsConstructor;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.rollup.TsSearcher;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.rest.metrics.*;
import org.okapi.rest.metrics.query.GetMetricsRequestInternal;
import org.okapi.rest.metrics.query.GetMetricsResponse;
import org.okapi.rest.metrics.query.ListMetricsRequest;
import org.okapi.rest.metrics.query.ListMetricsResponse;
import org.okapi.rest.metrics.search.SearchMetricsRequestInternal;
import org.okapi.rest.metrics.search.SearchMetricsResponse;

@AllArgsConstructor
public class QueryProcImpl implements QueryProcessor {
  TsReader tsReader;
  TsSearcher tsSearcher;

  @Override
  public GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) {
    var start = request.getStart();
    var end = request.getEnd();
    var res = request.getResolution();
    var path = MetricPaths.convertToUnivPath(request);
    var agg = request.getAggregation();
    var scanResult = tsReader.scanGauge(path, start, end, agg, res);
    return GetMetricsResponse.builder()
        .name(request.getMetricName())
        .tags(request.getTags())
        .resolution(request.getResolution())
        .aggregation(request.getAggregation())
        .tenant(request.getTenantId())
        .times(scanResult.getTimestamps())
        .values(scanResult.getValues())
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

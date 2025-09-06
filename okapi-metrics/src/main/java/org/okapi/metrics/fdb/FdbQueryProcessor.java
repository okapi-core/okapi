package org.okapi.metrics.fdb;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.metrics.service.web.QueryProcessor;
import org.okapi.rest.metrics.*;
import org.rocksdb.RocksDBException;

@AllArgsConstructor
public class FdbQueryProcessor implements QueryProcessor {
  TsReader tsReader;

  @Override
  public GetMetricsResponse getMetricsResponse(GetMetricsRequestInternal request) throws Exception {
    var start = request.getStart();
    var end = request.getEnd(); var res = request.getResolution();
    var path = MetricPaths.convertToPath(request);
    var agg = request.getAggregation();
    var scanResult = tsReader.scan(path, start, end, agg, res);
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
      SearchMetricsRequestInternal searchMetricsRequest)
      throws BadRequestException, RocksDBException, IOException {
    // list all metrics under the inverted path
    return null;
  }
}

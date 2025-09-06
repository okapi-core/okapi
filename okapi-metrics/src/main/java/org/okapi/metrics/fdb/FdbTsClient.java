package org.okapi.metrics.fdb;

import java.util.Map;
import lombok.AllArgsConstructor;
import org.okapi.Statistics;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rollup.TsReader;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TimeseriesClient;

@AllArgsConstructor
public class FdbTsClient implements TimeseriesClient {
  String tenantId;
  TsReader tsReader;

  @Override
  public Map<Long, Statistics> get(
      String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
    var universalPath = MetricPaths.convertToPath(tenantId, name, tags);
    RES_TYPE resolution =
        switch (res) {
          case SECONDLY -> RES_TYPE.SECONDLY;
          case MINUTELY -> RES_TYPE.MINUTELY;
          case HOURLY -> RES_TYPE.HOURLY;
        };
    var scanResult = tsReader.scan(universalPath, startMs, endMs, resolution);
    return scanResult;
  }
}

package org.okapi.metrics.query.promql;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.okapi.Statistics;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.metrics.rocks.RocksDbReader;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.rollup.RocksTsReader;
import org.okapi.metrics.stats.StatisticsRestorer;
import org.okapi.promql.eval.ts.RESOLUTION;
import org.okapi.promql.eval.ts.TimeseriesClient;

public class RocksMetricsClient implements TimeseriesClient {

  ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  PathRegistry pathRegistry;
  RocksStore rocksStore;
  StatisticsRestorer<Statistics> unmarshaller;

  @Override
  public Map<Long, Statistics> get(
      String name, Map<String, String> tags, RESOLUTION res, long startMs, long endMs) {
    var path = MetricPaths.convertToPath(name, tags);
    var shard = shardsAndSeriesAssigner.getShard(path);
    var dbPath = pathRegistry.rocksPath(shard);
    RocksDbReader rocksReader;
    try {
      var maybeReader = rocksStore.rocksReader(dbPath);
      if (maybeReader.isPresent()) {
        rocksReader = maybeReader.get();
      } else {
        return Collections.emptyMap();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    var tsReader = new RocksTsReader(rocksReader, unmarshaller);
    RES_TYPE scanResolution =
        switch (res) {
          case HOURLY -> RES_TYPE.HOURLY;
          case MINUTELY -> RES_TYPE.MINUTELY;
          case SECONDLY -> RES_TYPE.SECONDLY;
        };
    var result = tsReader.scan(path, startMs, endMs, scanResolution);
    return result;
  }
}

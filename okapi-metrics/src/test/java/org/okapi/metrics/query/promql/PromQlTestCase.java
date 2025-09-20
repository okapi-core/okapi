package org.okapi.metrics.query.promql;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import org.okapi.collections.OkapiLists;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.stats.StatisticsFrozenException;

public class PromQlTestCase {
  Map<Integer, Map<String, TreeMap<Long, Float>>> values;
  public static final MetricsContext FIXED_CTX = MetricsContext.createContext("test");

  public PromQlTestCase() {
    this.values = new HashMap<>();
  }

  void set(int shard, String series, long ts, float val) {
    var shardMap = values.computeIfAbsent(shard, (k) -> new HashMap<>());
    var tsMap = shardMap.computeIfAbsent(series, (s) -> new TreeMap<>());
    tsMap.put(ts, val);
  }

  void setBatch(int shard, String series, long[] ts, float[] vals) {
    for (int i = 0; i < ts.length; i++) {
      set(shard, series, ts[i], vals[i]);
    }
  }

  public Optional<Float> get(int shard, String series, long ts) {
    if (values.containsKey(shard)) {
      var shardMap = values.get(shard);
      if (shardMap.containsKey(series)) {
        var tsMap = shardMap.get(series);
        return Optional.ofNullable(tsMap.get(ts));
      }
    }
    return Optional.empty();
  }

  void setup(TestResourceFactory resourceFactory, Node node)
      throws StatisticsFrozenException, OutsideWindowException, IOException, InterruptedException {
    var shardMap = resourceFactory.shardMap(node);
    for (var shard : values.keySet()) {
      for (var path : values.get(shard).keySet()) {
        var ts = values.get(shard).get(path).keySet().stream().toList();
        var vals = ts.stream().map(t -> values.get(shard).get(path).get(t)).toList();
        shardMap.apply(
            shard, FIXED_CTX, path, OkapiLists.toLongArray(ts), OkapiLists.toFloatArray(vals));
      }
    }

    var box = resourceFactory.messageBox(node);
    shardMap.flushAll();
    await().atMost(Duration.of(1, ChronoUnit.SECONDS)).until(() -> box.isEmpty());
  }

  public PromQlQueryProcessor queryProcessor(
      TestResourceFactory resourceFactory, ExecutorService executorService, Node node)
      throws IOException {
    var discovery = resourceFactory.casDiscoveryFactory(node);
    TsClientFactory clientFactoy = resourceFactory.tsClientFactory(node);
    var queryProcessor =
        new PromQlQueryProcessor(
            executorService, resourceFactory.statisticsMerger(), clientFactoy, discovery);
    return queryProcessor;
  }
}

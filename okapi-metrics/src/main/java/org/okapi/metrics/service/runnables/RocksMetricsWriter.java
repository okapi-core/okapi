package org.okapi.metrics.service.runnables;

import static org.okapi.metrics.common.MetricPaths.convertToPath;
import static org.okapi.metrics.common.MetricPaths.getMetricsContext;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.rocksdb.RocksDB;

@Slf4j
public class RocksMetricsWriter implements MetricsWriter {

  ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  AtomicBoolean isReady = new AtomicBoolean(false);
  ShardMap shardMap;
  ServiceController serviceController;
  String self;

  Map<Path, RocksDB> dbCache;

  public RocksMetricsWriter(ShardMap shardMap, ServiceController serviceController, String self) {
    this.shardMap = shardMap;
    this.serviceController = serviceController;
    this.self = self;
    this.dbCache = new HashMap<>();
    isReady.set(true);
  }

  @Override
  public void onRequestArrive(SubmitMetricsRequestInternal request)
      throws BadRequestException,
          OutsideWindowException,
          InterruptedException,
          StatisticsFrozenException {
    if (!isReady()) throw new IllegalStateException("Cannot accept writes until ready.");
    if (request == null) {
      throw new BadRequestException("Request is null.");
    }
    if (!serviceController.canConsume()) {
      throw new BadRequestException("Cannot process request as cluster is possibly resharding.");
    }
    if (shardsAndSeriesAssigner == null) {
      throw new BadRequestException("Request sent before sharding is ready.");
    }
    // Shard / node routing
    final String path = convertToPath(request);
    final int shard = shardsAndSeriesAssigner.getShard(path);
    final String node = shardsAndSeriesAssigner.getNode(shard);
    if (!self.equals(node)) {
      throw new BadRequestException("Metric doesn't belong to this node.");
    }

    // Build context once
    final var ctx = getMetricsContext(request);
    try {
      shardMap.apply(shard, ctx, path, request.getTs(), request.getValues());
    } catch (OutsideWindowException e) {
      throw new BadRequestException();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setShardsAndSeriesAssigner(ShardsAndSeriesAssigner shardsAndSeriesAssigner) {
    this.shardsAndSeriesAssigner = shardsAndSeriesAssigner;
  }

  @Override
  public boolean isReady() {
    return shardsAndSeriesAssigner != null && isReady.get();
  }

  @Override
  public void init() throws IOException, StreamReadingException {}
}

package org.okapi.metrics.service.runnables;

import static org.okapi.metrics.common.MetricPaths.convertToPath;
import static org.okapi.metrics.common.MetricPaths.getMetricsContext;

import org.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.constants.Constants;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.paths.PathSet;
import org.okapi.metrics.service.ServiceController;
import org.rocksdb.RocksDB;

@Slf4j
public class RocksMetricsWriter implements MetricsWriter {

  @Setter ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  AtomicBoolean isReady = new AtomicBoolean(false);
  ShardMap shardMap;
  ServiceController serviceController;
  String self;
  PathSet pathSet;

  Map<Path, RocksDB> dbCache;

  public RocksMetricsWriter(
      ShardMap shardMap, ServiceController serviceController, String self, PathSet pathSet) {
    this.shardMap = shardMap;
    this.serviceController = serviceController;
    this.self = self;
    this.dbCache = new HashMap<>();
    this.pathSet = pathSet;
    isReady.set(true);
  }

  @Override
  public void onRequestArrive(SubmitMetricsRequestInternal request) throws BadRequestException {

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
    if (shardsAndSeriesAssigner == null) {
      log.error("{} received a null ShardsAndSeriesAssigner.", self);
    }
    this.shardsAndSeriesAssigner = shardsAndSeriesAssigner;
    var shards = new HashSet<Integer>();
    IntStream.range(0, Constants.N_SHARDS)
            .filter(shard -> Objects.equals(this.shardsAndSeriesAssigner.getNode(shard), self))
            .forEach(shards::add);
    this.pathSet.setShards(Collections.unmodifiableSet(shards));
  }

  @Override
  public boolean isReady() {
    if (shardsAndSeriesAssigner == null) {
      log.error("{} is not ready because shardsAssigner is not provided.", self);
    }
    return shardsAndSeriesAssigner != null && isReady.get();
  }

  @Override
  public void init() {
    if (shardsAndSeriesAssigner == null) {
      throw new IllegalStateException("Cannot call init without setting ShardsAndSeriesAssigner.");
    }
  }
}

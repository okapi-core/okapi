package org.okapi.metrics.service.runnables;

import static org.okapi.metrics.common.MetricPaths.convertToPath;
import static org.okapi.metrics.common.MetricPaths.getMetricsContext;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Setter;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.service.ServiceController;
import org.okapi.metrics.stats.StatisticsFrozenException;

public class PeriodicSnapshotWriter implements MetricsWriter {

  @Setter ShardsAndSeriesAssigner shardsAndSeriesAssigner;
  ShardMap shardMap;
  ServiceController serviceController;
  final String self;
  ScheduledExecutorService scheduledExecutorService;
  Checkpointer checkpointer;
  AtomicBoolean initialized = new AtomicBoolean(false);

  public PeriodicSnapshotWriter(
      ShardMap shardMap,
      ServiceController serviceController,
      String self,
      ScheduledExecutorService scheduledExecutorService,
      Checkpointer checkpointer) {
    this.shardMap = shardMap;
    this.serviceController = serviceController;
    this.self = self;
    this.scheduledExecutorService = scheduledExecutorService;
    this.checkpointer = checkpointer;
  }

  @Override
  public void init() throws IOException, StreamReadingException {
    var main = this.checkpointer.getSnapshotPath();
    if (Files.exists(main)) {
      shardMap.reset(main);
    }
    this.scheduledExecutorService.schedule(this.checkpointer, 0, TimeUnit.SECONDS);
    initialized.set(true);
  }

  public boolean isInitialized() {
    return this.initialized.get() && this.shardsAndSeriesAssigner != null;
  }

  @Override
  public void onRequestArrive(SubmitMetricsRequestInternal request)
      throws BadRequestException {
    if (!isInitialized()) throw new IllegalStateException("Cannot accept writes until ready.");
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
    } catch (StatisticsFrozenException e) {
        throw new RuntimeException(e);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
  }

  @Override
  public boolean isReady() {
    return this.isInitialized();
  }
}

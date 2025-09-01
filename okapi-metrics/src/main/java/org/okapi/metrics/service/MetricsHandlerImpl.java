package org.okapi.metrics.service;

import static org.okapi.constants.Constants.N_SHARDS;

import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.metrics.CheckpointUploaderDownloader;
import org.okapi.metrics.PathRegistry;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.async.Await;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;
import org.okapi.metrics.query.promql.TimeSeriesClientFactory;
import org.okapi.metrics.rocks.RocksStore;
import org.okapi.metrics.service.runnables.*;
import org.okapi.metrics.sharding.ShardPkgManager;

@AllArgsConstructor
@Slf4j
public class MetricsHandlerImpl implements MetricsHandler, Shardable {
  public static final Duration POLL_DURATION = Duration.of(100, ChronoUnit.MILLIS);
  public static final Duration MAX_WAIT = Duration.of(60, ChronoUnit.SECONDS);

  ServiceRegistry serviceRegistry;
  PathRegistry pathRegistry;
  CheckpointUploaderDownloader checkpointUploader;
  ServiceController serviceController;
  MetricsWriter metricsWriter;

  // background jobs
  HeartBeatReporterRunnable heartBeatWriter;
  LeaderResponsibilityRunnable leaderResponsibilityRunnable;
  CheckpointUploader hourlyCheckpointUploaderRunnable;

  ScheduledExecutorService scheduler;
  ShardsAndSeriesAssignerFactory shardsAndSeriesAssignerFactory;

  // map from dataset to shards
  ShardMap shardMap;
  ShardPkgManager shardPkgManager;

  // Rocks store to close when moving shards
  RocksStore rocksStore;

  // TimeSeriesFactory needs to know what are the shards
  TimeSeriesClientFactory timeSeriesClientFactory;

  @Override
  public void onStart() throws Exception {
    serviceRegistry.registerMetricProcessor();
    // report state as started
    serviceRegistry.setSelfState(NodeState.METRICS_CONSUMPTION_START);
    serviceController.startProcess();
    var nodes = serviceRegistry.listActiveNodes();
    var nShards = shardMap.getNsh();
    var assigner =
        shardsAndSeriesAssignerFactory.makeAssigner(nShards, nodes.orElse(Collections.emptyList()));
    metricsWriter.setShardsAndSeriesAssigner(assigner);
    timeSeriesClientFactory.setShardsAndSeriesAssigner(assigner);
    scheduler.schedule(heartBeatWriter, 0, TimeUnit.SECONDS);
    scheduler.schedule(leaderResponsibilityRunnable, 0, TimeUnit.SECONDS);
    scheduler.schedule(hourlyCheckpointUploaderRunnable, 0, TimeUnit.SECONDS);
    metricsWriter.init();
  }

  @Override
  public void onStop() throws Exception {
    serviceController.stopProcess();
    serviceRegistry.setSelfState(NodeState.PROCESS_STOPPED);
  }

  @Override
  public void onShardMovePrepare() throws Exception {
    /**
     * Find out the new list of nodes for this cluster. Upload a checkpoint of all shards that this
     * node holds.
     */
    if (myState(NodeState.SHARD_CHECKPOINTS_UPLOADED)) return;
    /** pause the consumer */
    serviceController.pauseConsumer();

    /** flush shardMap so that all pending writes are written out */
    shardMap.flushAll();

    /** wait for messages to be written out to rocksDB */
    Await.waitFor(
        () -> {
          return serviceController.isBoxEmpty();
        },
        POLL_DURATION,
        MAX_WAIT);
    /** close the store to prevent any stale instances */
    rocksStore.close();

    var self = serviceRegistry.getSelf();
    var newClusterConfig = serviceRegistry.clusterChangeOp().get();
    var oldConfig = serviceRegistry.oldClusterConfig().get();
    var oldAssignment = shardsAndSeriesAssignerFactory.makeAssigner(N_SHARDS, oldConfig.nodes());
    var wereMyShards =
        IntStream.range(0, N_SHARDS)
            .filter(i -> Objects.equals(oldAssignment.getNode(i), self.id()))
            .toArray();
    var newAssignment =
        shardsAndSeriesAssignerFactory.makeAssigner(N_SHARDS, newClusterConfig.nodes());
    var notMyShards =
        Arrays.stream(wereMyShards)
            .filter(i -> !Objects.equals(newAssignment.getNode(i), self.id()))
            .toArray();
    for (var shard : notMyShards) {
      var rocksPath = pathRegistry.rocksPath(shard);
      if (!Files.exists(rocksPath)) {
        continue;
      }
      var packagePath = shardPkgManager.packageShard(shard);
      checkpointUploader.uploadShardCheckpoint(packagePath, newClusterConfig.opId(), shard);
    }
    serviceRegistry.setSelfState(NodeState.SHARD_CHECKPOINTS_UPLOADED);
  }

  @Override
  public void onShardMoveRollback() throws Exception {
    /**
     * Use the old set of nodes. Calculate shards that will be held by this node. Reset Shardmap and
     * load old distribution
     */
    if (myState(NodeState.ROLLED_BACK)) return;
    afterShardMoveRollback();
    serviceRegistry.setSelfState(NodeState.ROLLED_BACK);
  }

  @Override
  public void onShardMoveCommit() throws Exception {
    /**
     * Use the new set of nodes. Calculate shards that will be held by this node. Reset Shardmap and
     * load new distribution
     */
    if (myState(NodeState.SHARD_CHECKPOINTS_APPLIED)) {
      return;
    }
    var self = serviceRegistry.getSelf();
    var scaleOp = serviceRegistry.clusterChangeOp().get();
    var newNodeList = serviceRegistry.latestClusterConfig().nodes();
    var nShards = N_SHARDS;
    var newNodes = shardsAndSeriesAssignerFactory.makeAssigner(nShards, newNodeList);
    var myShards =
        IntStream.range(0, nShards)
            .filter(i -> Objects.equals(newNodes.getNode(i), self.id()))
            .toArray();

    for (var shard : myShards) {
      var downloadPath = pathRegistry.shardPackagePath(shard);
      checkpointUploader.downloadShardCheckpoint(scaleOp.opId(), shard, downloadPath);
      if (!Files.exists(downloadPath) || Files.size(downloadPath) == 0) continue;
      shardPkgManager.unpackShard(downloadPath, shard);
    }
    afterShardMoveCommit();
    serviceRegistry.setSelfState(NodeState.SHARD_CHECKPOINTS_APPLIED);
  }

  @Override
  public void afterShardMoveCommit() throws Exception {
    var nShards = N_SHARDS;
    var nodes = serviceRegistry.latestClusterConfig().nodes();

    if (nodes.isEmpty()) {
      throw new IllegalStateException(
          "List of nodes is empty. Possibly cluster is being scaled while being resharded.");
    }

    var assigner = shardsAndSeriesAssignerFactory.makeAssigner(nShards, nodes);
    metricsWriter.setShardsAndSeriesAssigner(assigner);
    timeSeriesClientFactory.setShardsAndSeriesAssigner(assigner);
    serviceController.resumeConsumer();
  }

  @Override
  public void afterShardMoveRollback() throws Exception {
    var oldNodeConfig = serviceRegistry.oldClusterConfig();
    if (oldNodeConfig.isEmpty()) {
      throw new IllegalStateException(
          "List of nodes is empty. This is unexpected. Rollback will fail, operator intervention required.");
    }

    var assigner =
        shardsAndSeriesAssignerFactory.makeAssigner(N_SHARDS, oldNodeConfig.get().nodes());
    metricsWriter.setShardsAndSeriesAssigner(assigner);
    timeSeriesClientFactory.setShardsAndSeriesAssigner(assigner);
    serviceController.resumeConsumer();
  }

  private boolean myState(NodeState expected) throws Exception {
    var self = serviceRegistry.getSelf();
    if (self.state() == expected) {
      log.info("Already on state: {}", expected);
      return true;
    } else return false;
  }
}

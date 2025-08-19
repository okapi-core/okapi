package org.okapi.metrics.common;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.okapi.clock.Clock;
import org.okapi.metrics.common.pojo.*;

@Slf4j
public class ServiceRegistryImpl implements ServiceRegistry {

  @Getter ZkResources zkResources;
  @Getter
  Node node;
  Gson gson;
  @Getter FleetMetadata fleetMetadata;
  @Getter Clock clock;

  public ServiceRegistryImpl(
      Clock clock, FleetMetadata fleetMetadata, Node node, ZkResources zkResources) {
    this.clock = clock;
    this.fleetMetadata = fleetMetadata;
    this.node = node;
    this.zkResources = zkResources;
    this.gson = new Gson();
  }

  @Override
  public void registerMetricProcessor() throws Exception {
    var zkPath = ZkPaths.metricsProcessor(node.id());
    var gson = new Gson();
    var json = gson.toJson(node);
    fleetMetadata.createPathIfNotExists(zkPath);
    fleetMetadata.setData(zkPath, json.getBytes());
  }

  @Override
  public Optional<List<String>> listActiveNodes() throws Exception {
    // active nodes must be registered from a 2pc phase else it doesn't work
    var maybeClusterChangeOp = clusterChangeOp();
    if (maybeClusterChangeOp.isEmpty()) {
      return Optional.empty();
    }
    var clusterChangeOp = maybeClusterChangeOp.get();
    return switch (clusterChangeOp.state()) {
      case TWO_PHASE_STATE.FAILED -> Optional.of(oldClusterConfig().get().nodes());
      case TWO_PHASE_STATE.DONE -> Optional.of(latestClusterConfig().nodes());
      default -> Optional.empty();
    };
  }

  @Override
  public Set<String> listRegisteredMetricNodes() throws Exception {
    return Set.copyOf(fleetMetadata.listChildren(ZkPaths.metricsProcessorRoot()));
  }

  @Override
  public Node getNode(String id) throws Exception {
    var zkPath = ZkPaths.metricsProcessor(id);
    var data = fleetMetadata.getData(zkPath);
    return gson.fromJson(new String(data), Node.class);
  }
  
  @Override
  public void writeHeartBeat() throws Exception {
    var path = ZkPaths.heartbeatPath(node.id());
    fleetMetadata.createParentsIfNeeded(path);
    fleetMetadata.setData(path, Long.toString(clock.currentTimeMillis()).getBytes());
  }

  @Override
  public Optional<Long> readHeartBeat(String node) throws Exception {
    var path = ZkPaths.heartbeatPath(node);
    var heartbeat = fleetMetadata.getData(path);
    if (heartbeat == null || heartbeat.length == 0) {
      return Optional.empty();
    }
    return Optional.of(Long.parseLong(new String(heartbeat)));
  }

  @Override
  public void setSelfState(NodeState state) throws Exception {
    var zkPath = ZkPaths.metricsProcessor(node.id());
    var merged = new Node(node.id(), node.ip(), state);
    fleetMetadata.setData(zkPath, gson.toJson(merged).getBytes());
  }

  @Override
  public Node getSelf() throws Exception {
    var metadata = getNode(node.id());
    return metadata;
  }

  @Override
  public void incFailedHeartBeatCount(String nodeId) throws Exception {
    var zkPath = ZkPaths.failedCount(nodeId);
    fleetMetadata.incCounter(zkPath);
  }

  @Override
  public void decFailedHeartBeatCount(String nodeId) throws Exception {
    var zkPath = ZkPaths.failedCount(nodeId);
    fleetMetadata.decCounter(zkPath);
  }

  @Override
  public int getFailedHeartBeatCount(String nodeId) throws Exception {
    var zkPath = ZkPaths.failedCount(nodeId);
    return fleetMetadata.getCounter(zkPath);
  }

  @Override
  public void safelySetUnhealthyNodes(Collection<String> nodes) throws Exception {
    var csv = String.join(",", nodes);
    var couldAcquire = zkResources.clusterLock().acquire(10, TimeUnit.SECONDS);
    var zkPath = ZkPaths.unhealthyNodes();
    try {
      if (!couldAcquire) throw new IllegalStateException("Could not acquire cluster lock.");
      fleetMetadata.setData(zkPath, csv.getBytes());
    } finally {
      zkResources.clusterLock().release();
    }
  }

  @Override
  public List<String> getUnhealthyNodes() throws Exception {
    var zkPath = ZkPaths.unhealthyNodes();
    var data = fleetMetadata.getData(zkPath);
    if (data == null || data.length == 0) {
      return Collections.emptyList();
    }
    var csv = new String(data);
    return Arrays.asList(csv.split(","));
  }

  @Override
  public void safelyUpdateClusterOpState(String id, TWO_PHASE_STATE state) throws Exception {
    var couldAcquire = zkResources.clusterLock().acquire(10, TimeUnit.SECONDS);
    try {
      if (!couldAcquire) {
        return;
      }
      var currentState = clusterChangeOp();
      if (currentState.isEmpty()) {
        throw new IllegalArgumentException("No shardOp found to update.");
      }
      if (currentState.get().opId().equals(id)) {
        var updatedState = currentState.get().toBuilder().state(state).build();
        var json = gson.toJson(updatedState);
        fleetMetadata.setData(ZkPaths.clusterChangeOpPath(), json.getBytes());
      } else {
        throw new IllegalArgumentException("Could not update OpId state due to id mismatch.");
      }
    } finally {
      if (couldAcquire) {
        zkResources.clusterLock().release();
      }
    }
  }

  @Override
  public Optional<ClusterConfig> oldClusterConfig() throws Exception {
    return readAndCast(ZkPaths.oldNodeConfig(), ClusterConfig.class);
  }

  @Override
  public ClusterConfig latestClusterConfig() throws Exception {
    return readAndCast(ZkPaths.newNodeConfig(), ClusterConfig.class)
        .orElse(new ClusterConfig(DefaultShardConfig.OP_ID, Collections.singletonList(node.id())));
  }

  @Override
  public Optional<ClusterChangeOp> clusterChangeOp() throws Exception {
    return readAndCast(ZkPaths.clusterChangeOpPath(), ClusterChangeOp.class);
  }

  @Override
  public void safelyUpdateNodes(List<String> nodes) throws Exception {
    if (!zkResources.isLeader()) {
      throw new IllegalStateException("Cannot change cluster config if not leader.");
    }
    Preconditions.checkArgument(!nodes.isEmpty(), "Cluster cannot be empty");
    var couldAcquire = zkResources.clusterLock().acquire(10, TimeUnit.SECONDS);
    try {
      if (!couldAcquire) {
        return;
      }
      var maybeClusterConfigChangeOp = clusterChangeOp();
      if (maybeClusterConfigChangeOp.isPresent()
          && maybeClusterConfigChangeOp.get().state() != TWO_PHASE_STATE.DONE
          && maybeClusterConfigChangeOp.get().state() != TWO_PHASE_STATE.FAILED) {
        throw new IllegalStateException(
            "Cannot reshards while previous operation hasn't completed or failed.");
      }
      // cases -> present, failed; present, applied; not present -> assumed to have succeeded
      if (maybeClusterConfigChangeOp.isEmpty()) {
        var id = UUID.randomUUID().toString();
        var op =
            new ClusterChangeOp(
                id,
                nodes,
                TWO_PHASE_STATE.START,
                clock.currentTimeMillis(),
                clock.currentTimeMillis());
        var newConfig = new ClusterConfig(id, nodes);
        var oldConfig = new ClusterConfig(DefaultShardConfig.OP_ID, List.of(node.id()));
        var updatePaths =
            List.of(
                ZkPaths.clusterChangeOpPath(), ZkPaths.newNodeConfig(), ZkPaths.oldNodeConfig());
        var values =
            List.of(
                gson.toJson(op).getBytes(),
                gson.toJson(newConfig).getBytes(),
                gson.toJson(oldConfig).getBytes());
        fleetMetadata.atomicWrite(updatePaths, values);
        return;
      }
      var previousOp = maybeClusterConfigChangeOp.get();
      // if previous one has failed -> don't touch the old one, it is applied.
      if (previousOp.state() == TWO_PHASE_STATE.FAILED) {
        var id = UUID.randomUUID().toString();
        var op =
            new ClusterChangeOp(
                id,
                nodes,
                TWO_PHASE_STATE.START,
                clock.currentTimeMillis(),
                clock.currentTimeMillis());
        var newConfig = new ClusterConfig(id, nodes);
        var updatePaths = List.of(ZkPaths.clusterChangeOpPath(), ZkPaths.newNodeConfig());
        var values = List.of(gson.toJson(op).getBytes(), gson.toJson(newConfig).getBytes());
        fleetMetadata.atomicWrite(updatePaths, values);
        return;
      }

      var id = UUID.randomUUID().toString();
      var op =
          new ClusterChangeOp(
              id,
              nodes,
              TWO_PHASE_STATE.START,
              clock.currentTimeMillis(),
              clock.currentTimeMillis());
      var newConfig = new ClusterConfig(id, nodes);
      var backup = latestClusterConfig();
      var updatePaths =
          List.of(ZkPaths.clusterChangeOpPath(), ZkPaths.newNodeConfig(), ZkPaths.oldNodeConfig());
      var values =
          List.of(
              gson.toJson(op).getBytes(),
              gson.toJson(newConfig).getBytes(),
              gson.toJson(backup).getBytes());
      fleetMetadata.atomicWrite(updatePaths, values);
    } finally {
      if (couldAcquire) {
        zkResources.clusterLock().release();
      }
    }
  }

  private <T> Optional<T> readAndCast(String path, Class<T> clazz) throws Exception {
    var data = fleetMetadata.getData(path);
    if (data == null || data.length == 0) {
      return Optional.empty();
    }
    var dataAsStr = new String(fleetMetadata.getData(path));
    return Optional.ofNullable(gson.fromJson(dataAsStr, clazz));
  }
}

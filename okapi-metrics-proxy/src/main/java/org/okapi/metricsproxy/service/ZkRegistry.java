package org.okapi.metricsproxy.service;

import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.watch.PersistentWatcher;
import org.apache.zookeeper.Watcher;
import org.okapi.metrics.common.ServiceRegistry;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssignerFactory;

@Slf4j
public class ZkRegistry {
  PersistentWatcher clusterChangeWatcher;
  PersistentWatcher shardChangeWatcher;
  ServiceRegistry serviceRegistry;

  List<String> nodes;
  Map<String, Node> nodeMetadata;
  int shards;
  ShardsAndSeriesAssignerFactory assignerFactory;
  ShardsAndSeriesAssigner shardsAndSeriesAssigner;

  public ZkRegistry(
      CuratorFramework curatorFramework,
      ServiceRegistry serviceRegistry,
      ShardsAndSeriesAssignerFactory assignerFactory)
      throws Exception {
    var clusterChangePath = ZkPaths.clusterChangeOpPath();
    var shardChangePath = ZkPaths.shardOpPath();
    this.serviceRegistry = serviceRegistry;
    this.assignerFactory = assignerFactory;
    clusterChangeWatcher = new PersistentWatcher(curatorFramework, clusterChangePath, false);
    clusterChangeWatcher.getListenable().addListener(pathChangeListener());

    shardChangeWatcher = new PersistentWatcher(curatorFramework, shardChangePath, false);
    shardChangeWatcher.getListenable().addListener(pathChangeListener());
    updateClustersAndShards();
  }

  public Node route(String routingPath) throws Exception {
    if (shardsAndSeriesAssigner == null) {
      updateClustersAndShards();
    }
    if (shardsAndSeriesAssigner == null) {
      throw new NodesUnavailableException(
          "Cannot forward request due since cluster is unavailable.");
    }
    var shard = shardsAndSeriesAssigner.getShard(routingPath);
    var nodeId = shardsAndSeriesAssigner.getNode(shard);
    return nodeMetadata.get(nodeId);
  }

  public List<Node> listNodes() {
    return new ArrayList<>(nodeMetadata.values());
  }

  public Watcher pathChangeListener() {
    return (event) -> {
      try {
        updateClustersAndShards();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  public void updateClustersAndShards() throws Exception {
    log.info("Updating clusters.");
    nodes = serviceRegistry.listActiveNodes().orElse(Collections.emptyList());
    Map<String, Node> metadata = new HashMap<>();
    for (var node : nodes) {
      var md = serviceRegistry.getNode(node);
      metadata.put(node, md);
    }
    nodeMetadata = metadata;
    if (shards == -1 || nodes.isEmpty()) {
      shardsAndSeriesAssigner = null;
    } else shardsAndSeriesAssigner = assignerFactory.makeAssigner(shards, nodes);
  }
}

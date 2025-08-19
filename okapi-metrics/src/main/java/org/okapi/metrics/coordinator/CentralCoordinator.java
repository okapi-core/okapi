package org.okapi.metrics.coordinator;

import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.service.ServiceController;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class CentralCoordinator {
  ServiceController serviceController;
  CuratorFramework curatorFramework;
  String nodeType;

  public CentralCoordinator(
          ServiceController serviceController, CuratorFramework curatorFramework, String nodeType) {
    this.serviceController = serviceController;
    this.curatorFramework = curatorFramework;
    this.nodeType = nodeType;
  }

  public void registerWatchersForIdLoss(Node node) throws Exception {
    var ephemeralNodePath = ZkPaths.getEphemeralNodePath(nodeType, node);
    curatorFramework
        .getConnectionStateListenable()
        .addListener(
            (client, newState) -> {
              if (newState == ConnectionState.LOST) {
                serviceController.stopProcess();
              }
            });
    curatorFramework.getData().usingWatcher(new Watcher() {
      @Override
      public void process(WatchedEvent watchedEvent) {
        serviceController.stopProcess();
      }
    }).forPath(ephemeralNodePath);
  }
}

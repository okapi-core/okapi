package org.okapi.metrics.service.self;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.common.ZkPaths;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;

@Slf4j
public class ZkNodeCreator implements NodeCreator {
  CuratorFramework curatorFramework;
  Node node;
  IpSupplier ipSupplier;
  String nodeType;
  String idPath;

  public ZkNodeCreator(CuratorFramework curatorFramework, String nodeType, IpSupplier ipSupplier) {
    this.curatorFramework = curatorFramework;
    this.ipSupplier = ipSupplier;
    this.nodeType = nodeType;
  }

  public static final int ID_UPPER_LIMIT = 10_000;

  @SneakyThrows
  @Override
  public Node whoAmI() {
    ensureBasePath(curatorFramework, ZkPaths.getEphemeralNodeRoot(nodeType));
    for (int i = 0; i < ID_UPPER_LIMIT; i++) {
      idPath = ZkPaths.getEphemeralNodePath(nodeType, i);
      try {
        curatorFramework.create().withMode(CreateMode.EPHEMERAL).forPath(idPath);
        node =
            new Node(Integer.toString(i), ipSupplier.getIp(), NodeState.METRICS_CONSUMPTION_START);
        return node;
      } catch (KeeperException.NodeExistsException e) {
        log.info("Node with id-path {} exists, trying the next one", idPath);
      }
    }
    throw new RuntimeException("Id pool has been exhausted.");
  }

  private static void ensureBasePath(CuratorFramework client, String basePath) throws Exception {
    if (client.checkExists().forPath(basePath) == null) {
      client.create().creatingParentsIfNeeded().forPath(basePath);
    }
  }
}

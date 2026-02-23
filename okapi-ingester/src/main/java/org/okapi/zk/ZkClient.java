/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.zk;

import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

public class ZkClient {

  private final CuratorFramework client;

  public ZkClient(CuratorFramework client) {
    this.client = client;
  }

  public Stat writeNodeCas(String path, byte[] data, int expectedVersion) throws Exception {
    try {
      client.create().creatingParentsIfNeeded().forPath(path, data);
    } catch (KeeperException.NodeExistsException e) {
      // Node already exists, proceed to CAS update
    }
    return client.setData().withVersion(expectedVersion).forPath(path, data);
  }

  public byte[] readNode(String path, Stat statOut) throws Exception {
    return client.getData().storingStatIn(statOut).forPath(path);
  }

  // Convenience overload when you don't care about Stat
  public byte[] readNode(String path) throws Exception {
    return client.getData().forPath(path);
  }

  public void createEphemeral(String path, byte[] data) throws Exception {
    client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, data);
  }

  public void deleteNode(String path) throws Exception {
    client.delete().forPath(path);
  }

  // NEW: list children of a znode, e.g. /okapi/<ns>/shards
  public List<String> getChildren(String path) throws Exception {
    var doesNodeExist = client.checkExists().forPath(path);
    if (doesNodeExist == null) {
      return List.of();
    }
    return client.getChildren().forPath(path);
  }

  public boolean checkExists(String path) {
    try {
      var stat = client.checkExists().forPath(path);
      return stat != null;
    } catch (Exception e) {
      return false;
    }
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.sharding.listeners;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.okapi.sharding.ShardOrchestrator;
import org.okapi.zk.NamespacedZkClient;

@Slf4j
@AllArgsConstructor
public class ShardUpdateListener implements Closeable {

  private final ShardOrchestrator shardOrchestrator;
  private final NamespacedZkClient
      zkClient; // e.g. MetricsZkClient, has getAssignedShards() private final CuratorFramework
  // curatorFramework;
  private final String shardRoot; // e.g. zkPaths.getShardsPath()
  private final CuratorFramework curatorFramework;

  public ShardUpdateListener(
      ShardOrchestrator shardOrchestrator,
      NamespacedZkClient zkClient,
      CuratorFramework curatorFramework,
      String shardRoot) {
    this.shardOrchestrator = shardOrchestrator;
    this.zkClient = zkClient;
    this.curatorFramework = curatorFramework;
    this.shardRoot = shardRoot;
  }

  // Tracks which shards this node currently thinks it owns
  private final Set<Integer> currentAssignedShards = ConcurrentHashMap.newKeySet();

  private CuratorCache shardCache;

  /** Call this once at startup to begin watching shard config under shardRoot. */
  public void start() throws Exception {
    shardCache = CuratorCache.build(curatorFramework, shardRoot);

    shardCache
        .listenable()
        .addListener(
            (type, oldData, newData) -> {
              switch (type) {
                case NODE_CHANGED:
                case NODE_CREATED:
                case NODE_DELETED:
                  // Whenever shard znodes change, recompute assignments for this node
                  refreshShards();
                  break;

                default:
                  // ignore other events (SUSPENDED, LOST, etc.), they’re handled by Curator
                  // connection listeners
                  break;
              }
            });

    // Build initial cache and trigger INITIALIZED
    shardCache.start();
  }

  /**
   * Called whenever the *new* complete list of shards assigned to this node changes. Starts/stops
   * consumers accordingly.
   */
  public synchronized void refreshShards() {
    List<Integer> assigned = zkClient.getMyShards();
    Set<Integer> newAssigned = new HashSet<>(assigned);
    log.info("New assigned: {}", newAssigned);

    // 2. Stop consumers for shards that are no longer assigned
    for (Integer shardId : new HashSet<>(currentAssignedShards)) {
      if (!newAssigned.contains(shardId)) {
        try {
          shardOrchestrator.startMove(shardId);
          shardOrchestrator.completeMove(shardId);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }

    // 3. Update our local view
    currentAssignedShards.clear();
    currentAssignedShards.addAll(newAssigned);
  }

  @Override
  public void close() throws IOException {
    if (shardCache != null) {
      shardCache.close();
    }
  }
}

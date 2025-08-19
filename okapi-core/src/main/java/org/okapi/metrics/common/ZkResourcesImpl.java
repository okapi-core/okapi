package org.okapi.metrics.common;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

@Slf4j
public class ZkResourcesImpl implements ZkResources {
  CuratorFramework curatorFramework;
  InterProcessLock clusterLock;
  LeaderLatch leaderLatch;

  public ZkResourcesImpl(
      CuratorFramework curatorFramework, InterProcessLock clusterLock, LeaderLatch leaderLatch) {
    this.curatorFramework = curatorFramework;
    this.clusterLock = clusterLock;
    this.leaderLatch = leaderLatch;
    this.isLeader = new AtomicBoolean(false);
  }

  boolean isInitialized;
  AtomicBoolean isLeader;

  public void init() throws Exception {
    Preconditions.checkArgument(!isInitialized, "Cannot call initialize twice.");
    clusterLock = new InterProcessSemaphoreMutex(curatorFramework, ZkPaths.clusterLock());
    leaderLatch.addListener(
        new LeaderLatchListener() {
          @Override
          public void isLeader() {
            log.info("Leadership acquired");
            isLeader.set(true);
          }

          @Override
          public void notLeader() {
            log.info("Leadership lost");
            isLeader.set(false);
          }
        });
    leaderLatch.start();
    isInitialized = true;
  }

  @Override
  public InterProcessLock clusterLock() {
    return clusterLock;
  }

  @Override
  public boolean isLeader() throws Exception {
    return isLeader.get();
  }
}

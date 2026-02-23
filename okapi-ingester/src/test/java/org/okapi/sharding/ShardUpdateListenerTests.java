package org.okapi.sharding;

import java.nio.file.Path;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.okapi.sharding.listeners.ShardUpdateListener;
import org.okapi.zk.NamespacedZkClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ShardUpdateListenerTests {

  @Mock ShardOrchestrator shardOrchestrator;
  @Mock NamespacedZkClient zkClient;
  String shardRoot = "/okapi/test/shards";
  CuratorFramework curatorFramework;

  ShardUpdateListener listener;

  @BeforeEach
  void setup() throws Exception {
    var testingServer = new TestingServer();
    testingServer.start();
    curatorFramework =
        CuratorFrameworkFactory.builder()
            .connectString(testingServer.getConnectString())
            .retryPolicy(new ExponentialBackoffRetry(1000, 3))
            .build();
    curatorFramework.start();
    listener =
        new ShardUpdateListener(
            shardOrchestrator, zkClient, curatorFramework, shardRoot);
    Mockito.when(zkClient.getShardState(Mockito.eq(1))).thenReturn(getSteadyShard());
    Mockito.when(zkClient.getShardState(Mockito.eq(2))).thenReturn(getMovingShardMetadata());
    Mockito.when(zkClient.getShardState(Mockito.eq(3))).thenReturn(getShardWithHandoffOffset());
    Mockito.when(zkClient.getMyShards()).thenReturn(List.of(1, 2, 3));
  }

  @Test
  void testConsumerIsStartedForAssignedShardsOnStart() throws Exception {
    /// todo: fixme
    listener.start();
  }

  @Test
  void testConsumerIsRefreshedOnNodeUpdate() throws Exception {
    listener.start();
    var path = Path.of(shardRoot, "2").toString();
    curatorFramework.create().creatingParentsIfNeeded().forPath(path);
    curatorFramework.setData().forPath(path, "updated-data".getBytes());
  }

  @Test
  void testConsumerIsRefreshedOnNodeDelete() throws Exception {
    listener.start();
    var path = Path.of(shardRoot, "3").toString();
    curatorFramework.create().creatingParentsIfNeeded().forPath(path);
    curatorFramework.setData().forPath(path, "updated-data".getBytes());
    curatorFramework.delete().forPath(path);
  }

  @Test
  void testStaleShardsAreMoved() throws Exception {
    Mockito.when(zkClient.getMyShards()).thenReturn(List.of(1, 2, 3));
    listener.start();
    var path = Path.of(shardRoot, "2").toString();
    curatorFramework.create().creatingParentsIfNeeded().forPath(path);
    curatorFramework.setData().forPath(path, "updated-data".getBytes());
    Mockito.when(zkClient.getMyShards()).thenReturn(List.of(2));
    listener.refreshShards();
    Mockito.verify(shardOrchestrator).startMove(Mockito.eq(1));
    Mockito.verify(shardOrchestrator).completeMove(Mockito.eq(1));
    Mockito.verify(shardOrchestrator).startMove(Mockito.eq(3));
    Mockito.verify(shardOrchestrator).completeMove(Mockito.eq(3));
  }

  public static ShardMetadata getSteadyShard() {
    return ShardMetadata.builder()
        .epoch(0)
        .owner("node-1")
        .state(ShardState.STEADY)
        .target(null)
        .handoffOffset(null)
        .build();
  }

  public static ShardMetadata getMovingShardMetadata() {
    return ShardMetadata.builder()
        .epoch(1)
        .owner("node-1")
        .state(ShardState.MOVING)
        .target("node-2")
        .handoffOffset(100L)
        .build();
  }

  public static ShardMetadata getShardWithHandoffOffset() {
    return ShardMetadata.builder()
        .epoch(1)
        .owner("node-1")
        .state(ShardState.MOVING)
        .target("node-2")
        .handoffOffset(200L)
        .build();
  }
}

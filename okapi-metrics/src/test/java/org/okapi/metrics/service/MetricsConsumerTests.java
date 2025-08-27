package org.okapi.metrics.service;

import static org.awaitility.Awaitility.await;
import static org.okapi.constants.Constants.N_SHARDS;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.sharding.fakes.FixedShardsAndSeriesAssigner;

public class MetricsConsumerTests {

  TestResourceFactory testResourceFactory = new TestResourceFactory();
  Node testNode = new Node("test-node", "localhost:0", NodeState.METRICS_CONSUMPTION_START);
  Node testNode2 = new Node("test-node2", "localhost:1", NodeState.METRICS_CONSUMPTION_START);
  List<Node> farm;

  @BeforeEach
  public void setup() {
    farm = new ArrayList<>();
    farm.add(testNode);
    farm.add(testNode2);
  }

  @Test
  public void testStartStopLifecycle() throws Exception {
    var shardAssigner = new FixedShardsAndSeriesAssigner().assignShard(0, N_SHARDS, testNode.id());
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, Sets.newTreeSet(), shardAssigner)
        .set(N_SHARDS, Sets.newTreeSet(testNode.id()), shardAssigner);
    var handler1 = testResourceFactory.metricsHandler(farm.get(0));
    var startTime = testResourceFactory.clock(testNode).getTime();
    handler1.onStart();

    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var healthCheck =
                  testResourceFactory.serviceRegistry(testNode).readHeartBeat(testNode.id());
              return healthCheck.filter(aLong -> aLong - startTime < 5_000).isPresent();
            });
    var metadata = testResourceFactory.serviceRegistry(farm.get(1));
    var node1State = metadata.getNode(farm.get(0).id());
    assert node1State.state() == NodeState.METRICS_CONSUMPTION_START;
  }

  @Test
  public void testSanity() throws Exception {
    // should the update thread look-like.
    var shardAssigner =
        new FixedShardsAndSeriesAssigner()
            .assignShard(0, 1, testNode.id())
            .assignShard(1, N_SHARDS, testNode2.id())
            .assignSeries("tenant:test-metric{key1=value1}", 0)
            .assignSeries("tenant:test-metric{key2=value2}", 1);
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, new TreeSet<>(), shardAssigner)
        .set(N_SHARDS, Sets.newTreeSet(testNode.id()), shardAssigner)
        .set(N_SHARDS, Sets.newTreeSet(testNode2.id()), shardAssigner)
        .set(N_SHARDS, Sets.newTreeSet(testNode.id(), testNode2.id()), shardAssigner);
    var handler1 = testResourceFactory.metricsHandler(farm.get(0));
    var handler2 = testResourceFactory.metricsHandler(farm.get(1));
    var consumer1 = testResourceFactory.rocksWriter(farm.get(0));
    consumer1.setShardsAndSeriesAssigner(shardAssigner);
    var consumer2 = testResourceFactory.rocksWriter(farm.get(1));
    consumer2.setShardsAndSeriesAssigner(shardAssigner);
    // start both handlers
    handler1.onStart();
    handler2.onStart();

    // submit some mock requests
    consumer1.onRequestArrive(
        new SubmitMetricsRequestInternal(
            "tenant",
            "test-metric",
            Map.of("key1", "value1"),
            new float[] {1.0f, 2.0f},
            new long[] {1000L, 2000L}));
    consumer2.onRequestArrive(
        new SubmitMetricsRequestInternal(
            "tenant",
            "test-metric",
            Map.of("key2", "value2"),
            new float[] {1.0f, 2.0f},
            new long[] {1000L, 2000L}));
    var zkResources = testResourceFactory.zkResources(farm.get(0));
    zkResources.setLeader(true);

    // trigger a health check
    var leaderResponsibilities = testResourceFactory.leaderJobs(farm.get(0));
    leaderResponsibilities.checkFleetHealth();
  }

  @AfterEach
  public void tearDown() throws Exception {
    // Clean up resources if needed
    for (var consumer : farm) {
      try {
        var handler = testResourceFactory.metricsHandler(consumer);
        handler.onStop();
      } catch (Exception e) {
        System.err.println("Error stopping handler: " + e.getMessage());
      }
    }
  }
}

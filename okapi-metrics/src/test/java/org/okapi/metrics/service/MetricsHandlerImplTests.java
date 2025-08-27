package org.okapi.metrics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.constants.Constants.N_SHARDS;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.sharding.fakes.FixedShardsAndSeriesAssigner;
import org.okapi.metrics.stats.*;
import org.okapi.testutils.OkapiTestUtils;

@Slf4j
public class MetricsHandlerImplTests {
  TestResourceFactory testResourceFactory;
  public static final String TEST_NODE_1 = "test-node-1";
  public static final String TEST_NODE_2 = "test-node-2";
  public static final String TEST_NODE_3 = "test-node-3";
  Node testNode1;
  Node testNode2;
  Node testNode3;
  // series related methods
  Function<Integer, RollupSeries<Statistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<Statistics> statisticsSupplier;
  RollupSeriesRestorer<Statistics> restorer;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    testNode1 = testResourceFactory.makeNode(TEST_NODE_1);
    testNode2 = testResourceFactory.makeNode(TEST_NODE_2);
    testNode3 = testResourceFactory.makeNode(TEST_NODE_3);
    // series related stuff
    statsRestorer = new RolledupStatsRestorer();
    statisticsSupplier = new KllStatSupplier();
    seriesSupplier = new RollupSeriesFn();
    restorer = new RolledUpSeriesRestorer(seriesSupplier);
  }

  public void checkDistribution(
      RollupSeries<Statistics> ref,
      ShardsAndSeriesAssigner assigner,
      List<Node> nodes,
      int nShards) {
    for (var node : nodes) {
      // for this node should be empty
      // find all shards that belong to this node
      var shardsOfThisNode =
          IntStream.range(0, nShards)
              .filter(sh -> Objects.equals(assigner.getNode(sh), node.id()))
              .toArray();
      var shardsNotOfThisNode =
          IntStream.range(0, nShards)
              .filter(sh -> !Objects.equals(assigner.getNode(sh), node.id()))
              .toArray();
      var shardMap = testResourceFactory.shardMap(node);
      // INV: shards which are for this node: have same values as reference. Shards which are not
      // should be empty
      // INV: shards belonging to this node should have all metricpaths as per old distribution.
      for (var shard : shardsOfThisNode) {
        var expectedMetricPathsInShard =
            ref.listMetricPaths().stream()
                .filter(path -> assigner.getShard(path) == shard)
                .toList();
        assertEquals(
            Set.copyOf(expectedMetricPathsInShard),
            Set.copyOf(shardMap.get(shard).listMetricPaths()),
            "Expected paths differ for node: " + node.id());
        OkapiTestUtils.checkMatchesReferenceFuzzy(ref, shardMap.get(shard));
      }

      for (var shard : shardsNotOfThisNode) {
        shardMap.get(shard).getKeys().isEmpty();
      }
    }
  }

  public static Stream<Arguments> testScaleUpMethodSource() {
    var testNode1 = new Node(TEST_NODE_1, "localhost:1", NodeState.METRICS_CONSUMPTION_START);
    var testNode2 = new Node(TEST_NODE_2, "localhost:2", NodeState.METRICS_CONSUMPTION_START);
    var testNode3 = new Node(TEST_NODE_3, "localhost:3", NodeState.METRICS_CONSUMPTION_START);
    return Stream.of(
        // one empty node and one starting node.
        Arguments.of(
            testNode1,
            List.of(
                new SubmitMetricsRequestInternal(
                    "tenant",
                    "series-A",
                    Map.of("key1", "value1"),
                    new float[] {1.0f, 2.0f, 3.0f},
                    new long[] {1000, 2000, 3000})),
            Map.of("tenant:series-A{key1=value1}", 0),
            Map.of(0, testNode1.id()),
            Map.of(0, testNode2.id()),
            Arrays.asList(testNode1),
            Arrays.asList(testNode1, testNode2)),
        // 3 nodes two assigned shards, 2 are moved
        Arguments.of(
            testNode1,
            List.of(
                new SubmitMetricsRequestInternal(
                    "tenant",
                    "series-A",
                    Map.of("key1", "value1"),
                    new float[] {1.0f, 2.0f, 3.0f},
                    new long[] {1000, 2000, 3000}),
                new SubmitMetricsRequestInternal(
                    "tenant",
                    "series-B",
                    Map.of("key1", "value1"),
                    new float[] {1.0f, 2.0f, 3.0f},
                    new long[] {3000, 4000, 5000})),
            // shard distribution
            Map.of("tenant:series-A{key1=value1}", 0, "tenant:series-B{key1=value1}", 1),
            Map.of(0, testNode1.id(), 1, testNode2.id()),
            Map.of(0, testNode2.id(), 1, testNode3.id()),
            Arrays.asList(testNode1, testNode2),
            Arrays.asList(testNode1, testNode2, testNode3)));
  }

  @ParameterizedTest
  @MethodSource("testScaleUpMethodSource")
  public void testScaleUpCommit(
      Node leader,
      List<SubmitMetricsRequestInternal> requests,
      Map<String, Integer> seriesToShardMap,
      Map<Integer, String> oldDistribution,
      Map<Integer, String> newDistribution,
      List<Node> oldNodes,
      List<Node> newNodes)
      throws Exception {

    // initialize each node with its own distribution
    setupScaleUpTestData(
        leader, requests, seriesToShardMap, oldDistribution, newDistribution, oldNodes, newNodes);
    var newNodeIds = newNodes.stream().map(node -> node.id()).toList();
    var newAssigner = new FixedShardsAndSeriesAssigner(seriesToShardMap, newDistribution);
    var merged = seriesSupplier.apply(-1);
    merge(merged, requests);

    testResourceFactory.serviceRegistry(leader).safelyUpdateNodes(newNodeIds);
    for (var node : newNodes) {
      testResourceFactory.metricsHandler(node).onShardMovePrepare();
    }
    for (var node : newNodes) {
      var nodeState = testResourceFactory.serviceRegistry(node).getSelf().state();
      assertEquals(NodeState.SHARD_CHECKPOINTS_UPLOADED, nodeState);
    }
    for (var node : newNodes) {
      testResourceFactory.metricsHandler(node).onShardMoveCommit();
    }
    for (var node : newNodes) {
      var nodeState = testResourceFactory.serviceRegistry(node).getSelf().state();
      assertEquals(NodeState.SHARD_CHECKPOINTS_APPLIED, nodeState);
    }
    // check distribution wrt to new shard to node assignment
    checkDistribution(merged, newAssigner, newNodes, N_SHARDS);
  }

  public void setupScaleUpTestData(
      Node leader,
      List<SubmitMetricsRequestInternal> requests,
      Map<String, Integer> seriesToShardMap,
      Map<Integer, String> oldShardToNodeMap,
      Map<Integer, String> newShardToNodeMap,
      List<Node> oldNodes,
      List<Node> newNodes)
      throws Exception {
    testResourceFactory.zkResources(leader).setLeader(true);
    // mark all old-nodes as being registered
    var oldNodeIds = oldNodes.stream().map(node -> node.id()).toList();
    testResourceFactory.serviceRegistry(leader).safelyUpdateNodes(oldNodeIds);
    var opId = testResourceFactory.serviceRegistry(leader).clusterChangeOp().get().opId();
    testResourceFactory
        .serviceRegistry(leader)
        .safelyUpdateClusterOpState(opId, TWO_PHASE_STATE.DONE);

    // create all the necessary assigners
    var newNodeIds = newNodes.stream().map(node -> node.id()).toList();
    var newAssigner = new FixedShardsAndSeriesAssigner(seriesToShardMap, newShardToNodeMap);
    var oldAssigner = new FixedShardsAndSeriesAssigner(seriesToShardMap, oldShardToNodeMap);
    var reference = seriesSupplier.apply(-1);
    // setup shard assignments
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, new TreeSet<>(oldNodeIds), oldAssigner);
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, new TreeSet<>(newNodeIds), newAssigner);

    merge(reference, requests);
    // start and wait until all requests are consumed
    for (var node : newNodes) {}
    for (var node : newNodes) {
      testResourceFactory.metricsHandler(node).onStart();
    }
    // buffer all requests
    writeAll(oldNodes, requests, oldAssigner);
    // check distribution wrt to shard to node assignment
    checkDistribution(reference, oldAssigner, newNodes, N_SHARDS);
  }

  public static void merge(RollupSeries merged, List<SubmitMetricsRequestInternal> requests)
      throws OutsideWindowException, StatisticsFrozenException, InterruptedException {
    for (var r : requests) {
      var path = MetricPaths.convertToPath(r);
      merged.writeBatch(new MetricsContext("test"), path, r.getTs(), r.getValues());
    }
  }

  public static Node route(
      SubmitMetricsRequestInternal request, List<Node> nodes, ShardsAndSeriesAssigner assigner) {
    var path = MetricPaths.convertToPath(request);
    var recipient = assigner.getNode(assigner.getShard(path));
    return nodes.stream().filter(node -> node.id().equals(recipient)).findFirst().get();
  }

  public void writeAll(
      List<Node> nodes, List<SubmitMetricsRequestInternal> reqs, ShardsAndSeriesAssigner assigner)
      throws OutsideWindowException, BadRequestException, InterruptedException, StatisticsFrozenException {
    for (var r : reqs) {
      var node = route(r, nodes, assigner);
      testResourceFactory.rocksWriter(node).onRequestArrive(r);
    }
  }
}

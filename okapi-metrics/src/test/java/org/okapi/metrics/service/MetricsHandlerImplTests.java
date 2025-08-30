package org.okapi.metrics.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.constants.Constants.N_SHARDS;
import static org.okapi.metrics.GlobalTestConfig.okapiWait;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.okapi.Statistics;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.async.Await;
import org.okapi.metrics.common.MetricPaths;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.common.pojo.TWO_PHASE_STATE;
import org.okapi.metrics.common.sharding.ShardsAndSeriesAssigner;
import org.okapi.metrics.rollup.HashFns;
import org.okapi.metrics.rollup.RocksTsReader;
import org.okapi.metrics.rollup.RollupSeries;
import org.okapi.metrics.sharding.fakes.FixedShardsAndSeriesAssigner;
import org.okapi.metrics.stats.*;
import org.okapi.testutils.OkapiTestUtils;

/** todo: this is very complicated test, it needs refactoring so that the test fix */
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
  Function<Integer, RollupSeries<UpdatableStatistics>> seriesSupplier;
  StatisticsRestorer<Statistics> statsRestorer;
  Supplier<UpdatableStatistics> statisticsSupplier;
  RollupSeriesRestorer<UpdatableStatistics> restorer;

  static final long hrStart = (System.currentTimeMillis() / 3600_000L) * 3600_000L;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    testNode1 = testResourceFactory.makeNode(TEST_NODE_1);
    testNode2 = testResourceFactory.makeNode(TEST_NODE_2);
    testNode3 = testResourceFactory.makeNode(TEST_NODE_3);
    // series related stuff
    statsRestorer = new ReadonlyRestorer();
    statisticsSupplier = new KllStatSupplier();
    seriesSupplier = new RollupSeriesFn();
    restorer = new RolledUpSeriesRestorer(seriesSupplier);

    // use a real clock for all tests
    testResourceFactory.setUseRealClock(true);
  }

  public RocksTsReader getReaderBlocking(int shard, Node node) throws IOException {
    // check against a reader
    var pathRegistry = testResourceFactory.pathRegistry(node);
    okapiWait().until(() -> testResourceFactory.messageBox(node).isEmpty());
    log.info("Waiting for reader for shard: {} on node: {}", shard, node.id());
    okapiWait()
        .until(
            () ->
                testResourceFactory
                    .rocksStore(node)
                    .rocksReader(pathRegistry.rocksPath(shard))
                    .isPresent());
    var rocksReader =
        testResourceFactory.rocksStore(node).rocksReader(pathRegistry.rocksPath(shard)).get();
    return new RocksTsReader(rocksReader, testResourceFactory.readableUnmarshaller());
  }

  public void checkDistribution(
          RollupSeries<UpdatableStatistics> ref, ShardsAndSeriesAssigner assigner, List<Node> nodes, int nShards)
      throws IOException, InterruptedException {
    // \A key \in ref: shard = getShard(key); node = getNode(shard); reader = getReader(node,
    // shard); assertEquals(ref.getValue(key), reader.getValue(key));
    var unmarshaller = new ReadonlyRestorer();
    for (var key : ref.getKeys()) {
      var hashedValue = HashFns.invertKey(key);
      var series = hashedValue.get().timeSeries();
      var shard = assigner.getShard(series);
      var node = assigner.getNode(shard);
      var registered = testResourceFactory.getNode(node).get();
      var reader = getReaderBlocking(shard, registered);
      // do a blocking wait until the stats have been persisted
      Await.waitFor(
          () -> reader.getStat(key).isPresent(),
          Duration.of(100, ChronoUnit.MILLIS),
          Duration.of(10, ChronoUnit.SECONDS));
      var storedValue = reader.getStat(key).get();
      var expected = ref.getSerializedStats(key);
      var expectedRestored = unmarshaller.deserialize(expected);
      OkapiTestUtils.assertStatsEquals(expectedRestored, storedValue);
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
                    absoluteTime(new long[] {1000, 2000, 3000}))),
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
                    absoluteTime(new long[] {1000, 2000, 3000})),
                new SubmitMetricsRequestInternal(
                    "tenant",
                    "series-B",
                    Map.of("key1", "value1"),
                    new float[] {1.0f, 2.0f, 3.0f},
                    absoluteTime(new long[] {3000, 4000, 5000}))),
            // shard distribution
            Map.of("tenant:series-A{key1=value1}", 0, "tenant:series-B{key1=value1}", 1),
            Map.of(0, testNode1.id(), 1, testNode2.id()),
            Map.of(0, testNode2.id(), 1, testNode3.id()),
            Arrays.asList(testNode1, testNode2),
            Arrays.asList(testNode1, testNode2, testNode3)));
  }

  public static long[] absoluteTime(long[] relativeTime) {
    var absoluteTimes = new long[relativeTime.length];
    for (int i = 0; i < absoluteTimes.length; i++) {
      absoluteTimes[i] = hrStart + relativeTime[i];
    }
    return absoluteTimes;
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
    setupShardMoveTestData(
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

  public void setupShardMoveTestData(
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
    for (var node : newNodes) {
      testResourceFactory.metricsHandler(node).onStart();
    }
    // buffer all requests
    writeAll(oldNodes, requests, oldAssigner);
    for (var node : oldNodes) {
      testResourceFactory.startWriter(node);
    }
    // check distribution wrt to shard to node assignment
    checkDistribution(reference, oldAssigner, newNodes, N_SHARDS);
  }

  public static void merge(
          RollupSeries<UpdatableStatistics> merged, List<SubmitMetricsRequestInternal> requests)
      throws StatisticsFrozenException, InterruptedException {
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
      throws BadRequestException {
    for (var r : reqs) {
      var node = route(r, nodes, assigner);
      testResourceFactory.rocksWriter(node).onRequestArrive(r);
    }
  }
}

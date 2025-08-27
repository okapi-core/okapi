package org.okapi.metrics.service;

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.stats.StatisticsFrozenException;
import org.okapi.testutils.OkapiTestUtils;

@Execution(ExecutionMode.CONCURRENT)
public class ShardMapTests {
  TestResourceFactory testResourceFactory;
  Node TEST_NODE =
      new Node(OkapiTestUtils.smallId(4), "localhost", NodeState.METRICS_CONSUMPTION_START);
  public static final int SHARD = 1;

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
    testResourceFactory.setUseRealClock(true);
  }

  @Test
  public void testWriteHappyPath()
      throws StatisticsFrozenException, OutsideWindowException, IOException, InterruptedException {
    var shardMap = testResourceFactory.shardMap(TEST_NODE);
    var t0 = System.currentTimeMillis();
    var ts =
        new long[] {
          t0, t0 + 1000,
        };
    var vals = new float[] {0.1f, 0.2f};
    shardMap.apply(SHARD, MetricsContext.createContext("id"), "mp{}", ts, vals);
    var pathSet = testResourceFactory.pathSet(TEST_NODE);
    Assertions.assertTrue(pathSet.list().get(SHARD).contains("mp{}"));
    // check message box is empty
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              return !testResourceFactory.messageBox(TEST_NODE).isEmpty();
            });

    var writer = testResourceFactory.rocksDbStatsWriter(TEST_NODE);
    // start the writer
    writer.startWriting(
        testResourceFactory.scheduledExecutorService(TEST_NODE),
        testResourceFactory.rocksStore(TEST_NODE),
        testResourceFactory.writeBackSettings(TEST_NODE));
    // check message box get emptied
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              return testResourceFactory.messageBox(TEST_NODE).isEmpty();
            });
  }

  @Test
  public void testWriteFailsOnIllegaRequest() {
    var shardMap = testResourceFactory.shardMap(TEST_NODE);
    var t0 = System.currentTimeMillis();
    var ts =
        new long[] {
          t0, t0 + 1000,
        };
    var vals = new float[] {0.1f};
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> shardMap.apply(SHARD, MetricsContext.createContext("id"), "mp{}", ts, vals));
  }

  @Test
  public void testWriteFailsOnNull() {
    var shardMap = testResourceFactory.shardMap(TEST_NODE);
    var t0 = System.currentTimeMillis();
    var ts =
        new long[] {
          t0, t0 + 1000,
        };
    var vals = new float[] {0.1f};
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> shardMap.apply(SHARD, MetricsContext.createContext("id"), "mp{}", ts, null));
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> shardMap.apply(SHARD, MetricsContext.createContext("id"), "mp{}", null, vals));
  }

  @Test
  public void testThrowsIfOutsideWindow() {
    var shardMap = testResourceFactory.shardMap(TEST_NODE);
    var t0 = System.currentTimeMillis();
    var ts =
        new long[] {
          t0, t0 - 2 * Duration.of(1, ChronoUnit.HOURS).toMillis(),
        };
    var vals = new float[] {0.1f, 0.2f};
    Assertions.assertThrows(
        OutsideWindowException.class,
        () -> shardMap.apply(SHARD, MetricsContext.createContext("id"), "mp{}", ts, vals));
  }
}

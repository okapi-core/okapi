package org.okapi.metrics.runnables;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.okapi.constants.Constants.N_SHARDS;

import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.okapi.exceptions.BadRequestException;
import org.okapi.metrics.OutsideWindowException;
import org.okapi.metrics.ShardMap;
import org.okapi.metrics.TestResourceFactory;
import org.okapi.metrics.common.MetricsContext;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.sharding.fakes.FixedShardsAndSeriesAssigner;

public class PeriodicSnapshotWriterTests {
  TestResourceFactory testResourceFactory;
  Node testNode = new Node("id-1", "localhost:3000", NodeState.METRICS_CONSUMPTION_START);

  @BeforeEach
  public void setup() {
    testResourceFactory = new TestResourceFactory();
  }

  @Test
  public void testWriteSnapshotAfterMetricArrives()
      throws StreamReadingException, IOException, OutsideWindowException, BadRequestException, InterruptedException {
    var assigner =
        new FixedShardsAndSeriesAssigner()
            .assignSeries("tenant-A:latency{key1=value1}", 0)
            .assignShard(0, testNode.id());
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, Sets.newTreeSet(testNode.id()), assigner);
    testResourceFactory.scheduledExecutorService(testNode).setDelay(100);
    var writer = testResourceFactory.periodicSnapshotWriter(testNode);
    writer.init();
    writer.setShardsAndSeriesAssigner(assigner);
    var t = testResourceFactory.clock(testNode).getTime();
    var ts = new long[] {t, t + 1000, t + 2000};
    var vals = new float[] {0.1f, 0.2f, 0.1f};
    var request =
        SubmitMetricsRequestInternal.builder()
            .tenantId("tenant-A")
            .values(vals)
            .ts(ts)
            .tags(Map.of("key1", "value1"))
            .metricName("latency")
            .build();
    writer.onRequestArrive(request);
    var snapshotPath = testResourceFactory.checkpointer(testNode).getSnapshotPath();
    await()
        .atMost(Duration.of(1, ChronoUnit.SECONDS))
        .until(
            () -> {
              var size = Files.size(snapshotPath);
              return size > 0;
            });

    var map = new ShardMap(testResourceFactory.clock(testNode));
    map.reset(snapshotPath);

    // sanity check: metrics were written out.
    var metrics = map.get(0).listMetricPaths();
    assertFalse(metrics.isEmpty());
    assertTrue(metrics.contains("tenant-A:latency{key1=value1}"));
  }

  @Test
  public void testPeriodicWriterInit() throws OutsideWindowException, IOException, StreamReadingException {
    var assigner = new FixedShardsAndSeriesAssigner();
    testResourceFactory
        .shardsAndSeriesAssigner()
        .set(N_SHARDS, Sets.newTreeSet(testNode.id()), assigner);
    testResourceFactory.scheduledExecutorService(testNode).setDelay(100);
    var writer = testResourceFactory.periodicSnapshotWriter(testNode);
    var snapshotDir = testResourceFactory.checkpointer(testNode).getSnapshotPath();
    var clock = testResourceFactory.clock(testNode);
    var shardMap = new ShardMap(clock);
    var t = clock.currentTimeMillis();
    shardMap.get(0).writeBatch(MetricsContext.createContext("test-id"), "tenant-A:latency{service=app}",
            new long[]{t, t + 1000, t + 2000},
            new float[]{0.1f, 0.2f, 0.1f}
            );
    shardMap.snapshot(snapshotDir);
    assertFalse(writer.isReady());
    writer.init();
    writer.setShardsAndSeriesAssigner(assigner);
    assertTrue(writer.isReady());

    var loadedMap = testResourceFactory.shardMap(testNode);
    var metrics = loadedMap.get(0).listMetricPaths();
    assertFalse(metrics.isEmpty());
    assertTrue(metrics.contains("tenant-A:latency{service=app}"));
  }

}

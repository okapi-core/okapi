/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
import io.opentelemetry.proto.metrics.v1.Gauge;
import io.opentelemetry.proto.metrics.v1.Metric;
import io.opentelemetry.proto.metrics.v1.NumberDataPoint;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.metrics.v1.ScopeMetrics;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.CommonConfig;
import org.okapi.identity.TestWhoAmiFactory;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.sharding.ShardMetadata;
import org.okapi.sharding.ShardState;
import org.okapi.testmodules.LoggingMetricsBufferPool;
import org.okapi.testmodules.guice.TestMetricsConfig;
import org.okapi.testmodules.guice.TestMetricsModule;
import org.okapi.testmodules.guice.TestShardingModule;
import org.okapi.testmodules.guice.TestWalModule;
import org.okapi.zk.FakeZkClient;
import org.okapi.zk.NamespacedZkClient;

public class MetricsShardWalConsumerTests {

  @TempDir Path tempDir;
  private static final String REMOTE_NODE_ID = "remote-node";

  @Test
  void ingesterAndConsumerWorkTogetherViaWal() throws Exception {
    var assignedShards = List.of(0);
    Map<Long, Integer> blockShardMap = Map.of(0L, 0);
    MetricsCfg metricsCfg = new TestMetricsConfig(tempDir.resolve("metrics-data"));

    var injector =
        buildInjector(
            metricsCfg,
            tempDir.resolve("wal"),
            Path.of(metricsCfg.getDataDir()),
            assignedShards,
            assignedShards,
            blockShardMap);

    bootstrapShardState(injector, assignedShards, blockShardMap, assignedShards);

    var ingester = injector.getInstance(MetricsIngester.class);
    var driver = injector.getInstance(MetricsWalConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingMetricsBufferPool.class);
    var forwarder = injector.getInstance(FakeMetricsForwarder.class);

    ingester.ingestOtelProtobuf(buildRequest());
    driver.onTick();

    assertEquals(3, bufferPool.getCommands().size());
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void ingestsAcrossTwoShardsWhenAllAssigned() throws Exception {
    var assignedShards = List.of(0, 1);
    Map<Long, Integer> blockShardMap = Map.of(0L, 0, 1L, 1);
    MetricsCfg metricsCfg = new TestMetricsConfig(tempDir.resolve("metrics-data"));

    var injector =
        buildInjector(
            metricsCfg,
            tempDir.resolve("wal"),
            Path.of(metricsCfg.getDataDir()),
            assignedShards,
            assignedShards,
            blockShardMap);

    bootstrapShardState(injector, assignedShards, blockShardMap, assignedShards);

    var ingester = injector.getInstance(MetricsIngester.class);
    var driver = injector.getInstance(MetricsWalConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingMetricsBufferPool.class);
    var forwarder = injector.getInstance(FakeMetricsForwarder.class);

    ingester.ingestOtelProtobuf(buildTwoShardRequest());
    driver.onTick();

    assertEquals(2, bufferPool.getCommands().size());
    var streamIds =
        bufferPool.getCommands().stream().map(cmd -> cmd.streamId()).collect(Collectors.toSet());
    assertEquals(Set.of("0", "1"), streamIds);
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void forwardsUnassignedShardWhileConsumingAssigned() throws Exception {
    var assignedShards = List.of(0);
    var walShards = List.of(0, 1);
    Map<Long, Integer> blockShardMap = Map.of(0L, 0, 1L, 1);
    MetricsCfg metricsCfg = new TestMetricsConfig(tempDir.resolve("metrics-data"));

    var injector =
        buildInjector(
            metricsCfg,
            tempDir.resolve("wal"),
            Path.of(metricsCfg.getDataDir()),
            assignedShards,
            walShards,
            blockShardMap);

    bootstrapShardState(injector, assignedShards, blockShardMap, walShards);

    var ingester = injector.getInstance(MetricsIngester.class);
    var driver = injector.getInstance(MetricsWalConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingMetricsBufferPool.class);
    var forwarder = injector.getInstance(FakeMetricsForwarder.class);

    ingester.ingestOtelProtobuf(buildTwoShardRequest());
    driver.onTick();

    assertEquals(1, bufferPool.getCommands().size());
    assertEquals("0", bufferPool.getCommands().get(0).streamId());
    assertEquals(1, forwarder.getArgs().size());
    assertEquals(1, forwarder.getArgs().get(0).request().getMetricsRequests().size());
    assertEquals(1, forwarder.getArgs().get(0).request().getShardId());
  }

  private Injector buildInjector(
      MetricsCfg metricsCfg,
      Path walRoot,
      Path metricsDataDir,
      List<Integer> assignedShards,
      List<Integer> walShards,
      Map<Long, Integer> blockShardMap) {
    var shardingModule =
        new TestShardingModule(
            TestWhoAmiFactory.defaultTestWhoAmi(), assignedShards, blockShardMap, REMOTE_NODE_ID);
    var walModule = new TestWalModule(walRoot, walShards);
    var metricsModule = new TestMetricsModule(metricsCfg, metricsDataDir, 8);
    return Guice.createInjector(shardingModule, walModule, metricsModule);
  }

  private void bootstrapShardState(
      Injector injector,
      List<Integer> assignedShards,
      Map<Long, Integer> blockShardMap,
      List<Integer> shardsToInit) {
    var zk = (FakeZkClient) injector.getInstance(NamespacedZkClient.class);
    zk.updateAssigned(assignedShards);
    var whoAmI = TestWhoAmiFactory.defaultTestWhoAmi();
    var metadata = new HashMap<Integer, ShardMetadata>();
    var shardsWeCareAbout = new HashSet<>(shardsToInit);
    shardsWeCareAbout.addAll(blockShardMap.values());
    for (int shard = 0; shard < CommonConfig.N_SHARDS; shard++) {
      boolean assigned = assignedShards.contains(shard);
      boolean hasWal = shardsWeCareAbout.contains(shard);
      var owner = assigned ? whoAmI.getNodeId() : REMOTE_NODE_ID;
      var state = hasWal ? ShardState.STEADY : ShardState.MOVING;
      var target = hasWal ? null : whoAmI.getNodeId();
      metadata.put(
          shard,
          ShardMetadata.builder()
              .epoch(1L)
              .owner(owner)
              .state(state)
              .target(target)
              .handoffOffset(null)
              .build());
    }
    zk.updateAll(metadata);
  }

  private ExportMetricsServiceRequest buildRequest() {
    return ExportMetricsServiceRequest.newBuilder()
        .addResourceMetrics(
            ResourceMetrics.newBuilder()
                .addScopeMetrics(
                    ScopeMetrics.newBuilder()
                        .addMetrics(buildGaugeMetric("m1", 1_000_000_000L, 1.0))
                        .addMetrics(buildGaugeMetric("m2", 2_000_000_000L, 2.0))
                        .addMetrics(buildGaugeMetric("m3", 3_000_000_000L, 3.0)))
                .build())
        .build();
  }

  private ExportMetricsServiceRequest buildTwoShardRequest() {
    return ExportMetricsServiceRequest.newBuilder()
        .addResourceMetrics(
            ResourceMetrics.newBuilder()
                .addScopeMetrics(
                    ScopeMetrics.newBuilder()
                        .addMetrics(buildGaugeMetric("m1", 1_000_000_000L, 1.0))
                        .addMetrics(buildGaugeMetric("m2", 61_000_000_000L, 2.0)))
                .build())
        .build();
  }

  private Metric buildGaugeMetric(String name, long tsNanos, double value) {
    var dataPoint =
        NumberDataPoint.newBuilder()
            .setTimeUnixNano(tsNanos)
            .setStartTimeUnixNano(0L)
            .setAsDouble(value)
            .build();
    return Metric.newBuilder()
        .setName(name)
        .setGauge(Gauge.newBuilder().addDataPoints(dataPoint))
        .build();
  }
}

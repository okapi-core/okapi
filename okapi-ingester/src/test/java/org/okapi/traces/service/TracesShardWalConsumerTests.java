package org.okapi.traces.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.CommonConfig;
import org.okapi.identity.TestWhoAmiFactory;
import org.okapi.sharding.ShardMetadata;
import org.okapi.sharding.ShardState;
import org.okapi.testmodules.FakeTraceForwarder;
import org.okapi.testmodules.LoggingTracesBufferPool;
import org.okapi.testmodules.guice.TestTraceShardingModule;
import org.okapi.testmodules.guice.TestTracesConfig;
import org.okapi.testmodules.guice.TestTracesModule;
import org.okapi.testmodules.guice.TestTracesWalModule;
import org.okapi.traces.config.TracesCfg;
import org.okapi.zk.FakeZkClient;
import org.okapi.zk.NamespacedZkClient;

public class TracesShardWalConsumerTests {

  @TempDir Path tempDir;
  private static final String REMOTE_NODE_ID = "remote-node";

  @Test
  void ingesterAndConsumerWorkTogetherViaWal() throws Exception {
    var svc = "svcA";
    var assignedShards = List.of(0);
    var streamShardMap = Map.of(svc, 0);
    TracesCfg cfg = new TestTracesConfig(tempDir.resolve("traces-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            List.of(0),
            streamShardMap);

    bootstrapShardState(injector, assignedShards, List.of(0));

    var ingester = injector.getInstance(TracesIngester.class);
    var driver = injector.getInstance(TracesConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingTracesBufferPool.class);
    var forwarder = injector.getInstance(FakeTraceForwarder.class);

    ingester.ingest(buildRequest(Map.of(svc, List.of(buildSpan("span1", 1_000_000_000L)))));
    driver.onTick();

    assertEquals(1, bufferPool.getCommands().size());
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void ingestsAcrossTwoShardsWhenAllAssigned() throws Exception {
    var svcA = "svcA";
    var svcB = "svcB";
    var assignedShards = List.of(0, 1);
    var streamShardMap = Map.of(svcA, 0, svcB, 1);
    TracesCfg cfg = new TestTracesConfig(tempDir.resolve("traces-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            List.of(0, 1),
            streamShardMap);

    bootstrapShardState(injector, assignedShards, List.of(0, 1));

    var ingester = injector.getInstance(TracesIngester.class);
    var driver = injector.getInstance(TracesConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingTracesBufferPool.class);
    var forwarder = injector.getInstance(FakeTraceForwarder.class);

    ingester.ingest(
        buildRequest(
            Map.of(
                svcA, List.of(buildSpan("span1", 1_000_000_000L)),
                svcB, List.of(buildSpan("span2", 61_000_000_000L)))));
    driver.onTick();

    assertEquals(2, bufferPool.getCommands().size());
    var streams =
        bufferPool.getCommands().stream()
            .map(LoggingTracesBufferPool.ConsumeCommand::streamId)
            .collect(java.util.stream.Collectors.toSet());
    assertEquals(new HashSet<>(List.of("0", "1")), streams);
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void forwardsUnassignedShardWhileConsumingAssigned() throws Exception {
    var svcA = "svcA";
    var svcB = "svcB";
    var assignedShards = List.of(0);
    var walShards = List.of(0, 1);
    var streamShardMap = Map.of(svcA, 0, svcB, 1);
    TracesCfg cfg = new TestTracesConfig(tempDir.resolve("traces-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            walShards,
            streamShardMap);

    bootstrapShardState(injector, assignedShards, walShards);

    var ingester = injector.getInstance(TracesIngester.class);
    var driver = injector.getInstance(TracesConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingTracesBufferPool.class);
    var forwarder = injector.getInstance(FakeTraceForwarder.class);

    ingester.ingest(
        buildRequest(
            Map.of(
                svcA, List.of(buildSpan("span1", 1_000_000_000L)),
                svcB, List.of(buildSpan("span2", 61_000_000_000L)))));
    driver.onTick();

    assertEquals(1, bufferPool.getCommands().size());
    assertEquals("0", bufferPool.getCommands().get(0).streamId());
    assertEquals(1, forwarder.getArgs().size());
    assertEquals(1, forwarder.getArgs().get(0).record().getShard());
    assertEquals(1, forwarder.getArgs().get(0).record().getRecords().size());
  }

  private Injector buildInjector(
      TracesCfg cfg,
      Path walRoot,
      Path dataDir,
      List<Integer> assignedShards,
      List<Integer> walShards,
      Map<String, Integer> streamShardMap) {
    Map<Long, Integer> blockShardMap = Map.of();
    var shardingModule =
        new TestTraceShardingModule(
            TestWhoAmiFactory.defaultTestWhoAmi(),
            assignedShards,
            blockShardMap,
            streamShardMap,
            REMOTE_NODE_ID);
    var walModule = new TestTracesWalModule(walRoot, walShards);
    var tracesModule = new TestTracesModule(cfg, dataDir, 8);
    return Guice.createInjector(shardingModule, walModule, tracesModule);
  }

  private void bootstrapShardState(
      Injector injector, List<Integer> assignedShards, List<Integer> shardsToInit) {
    var zk = (FakeZkClient) injector.getInstance(NamespacedZkClient.class);
    zk.updateAssigned(assignedShards);
    var whoAmI = TestWhoAmiFactory.defaultTestWhoAmi();
    var metadata = new HashMap<Integer, ShardMetadata>();
    for (int shard = 0; shard < CommonConfig.N_SHARDS; shard++) {
      boolean assigned = assignedShards.contains(shard);
      boolean hasWal = shardsToInit.contains(shard);
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

  private ExportTraceServiceRequest buildRequest(Map<String, List<Span>> spansBySvc) {
    var builder = ExportTraceServiceRequest.newBuilder();
    for (var entry : spansBySvc.entrySet()) {
      var svc = entry.getKey();
      builder.addResourceSpans(
          ResourceSpans.newBuilder()
              .setResource(
                  Resource.newBuilder()
                      .addAttributes(
                          io.opentelemetry.proto.common.v1.KeyValue.newBuilder()
                              .setKey("service.name")
                              .setValue(
                                  io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                                      .setStringValue(svc)
                                      .build())
                              .build()))
              .addScopeSpans(ScopeSpans.newBuilder().addAllSpans(entry.getValue())));
    }
    return builder.build();
  }

  private Span buildSpan(String name, long startTsNanos) {
    var spanId = name + "-span";
    var traceId = name + "-trace";
    return Span.newBuilder()
        .setName(name)
        .setStartTimeUnixNano(startTsNanos)
        .setSpanId(ByteString.copyFromUtf8(spanId))
        .setTraceId(ByteString.copyFromUtf8(traceId))
        .build();
  }
}

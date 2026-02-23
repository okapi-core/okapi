package org.okapi.logs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.okapi.CommonConfig;
import org.okapi.identity.TestWhoAmiFactory;
import org.okapi.logs.config.LogsCfg;
import org.okapi.sharding.ShardMetadata;
import org.okapi.sharding.ShardState;
import org.okapi.testmodules.FakeLogForwarder;
import org.okapi.testmodules.LoggingLogsBufferPool;
import org.okapi.testmodules.guice.TestLogShardingModule;
import org.okapi.testmodules.guice.TestLogsConfig;
import org.okapi.testmodules.guice.TestLogsModule;
import org.okapi.testmodules.guice.TestLogsWalModule;
import org.okapi.zk.FakeZkClient;
import org.okapi.zk.NamespacedZkClient;

public class LogsShardWalConsumerTests {

  @TempDir Path tempDir;
  private static final String REMOTE_NODE_ID = "remote-node";

  @Test
  void ingesterAndConsumerWorkTogetherViaWal() throws Exception {
    var svc = "svcA";
    var streamId = 0;
    var assignedShards = List.of(0);
    var streamShardMap = Map.of(svc, 0);
    LogsCfg cfg = new TestLogsConfig(tempDir.resolve("logs-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            List.of(streamId),
            streamShardMap);

    bootstrapShardState(injector, assignedShards, List.of(0));

    var ingester = injector.getInstance(LogsIngester.class);
    var driver = injector.getInstance(LogsConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingLogsBufferPool.class);
    var forwarder = injector.getInstance(FakeLogForwarder.class);

    ingester.ingest(buildRequest(Map.of(svc, List.of(buildLogRecord("msg1", 1_000_000_000L)))));
    driver.onTick();

    assertEquals(1, bufferPool.getCommands().size());
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void ingestsAcrossTwoShardsWhenAllAssigned() throws Exception {
    var svcA = "svcA";
    var svcB = "svcB";
    var streamIds = List.of(0, 1);
    var assignedShards = List.of(0, 1);
    var streamShardMap = Map.of(svcA, 0, svcB, 1);
    LogsCfg cfg = new TestLogsConfig(tempDir.resolve("logs-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            streamIds,
            streamShardMap);

    bootstrapShardState(injector, assignedShards, List.of(0, 1));

    var ingester = injector.getInstance(LogsIngester.class);
    var driver = injector.getInstance(LogsConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingLogsBufferPool.class);
    var forwarder = injector.getInstance(FakeLogForwarder.class);

    ingester.ingest(
        buildRequest(
            Map.of(
                svcA, List.of(buildLogRecord("msg1", 1_000_000_000L)),
                svcB, List.of(buildLogRecord("msg2", 61_000_000_000L)))));
    driver.onTick();

    assertEquals(2, bufferPool.getCommands().size());
    var streams =
        bufferPool.getCommands().stream()
            .map(c -> c.streamId())
            .map(Integer::parseInt)
            .collect(java.util.stream.Collectors.toSet());
    assertEquals(new HashSet<>(streamIds), streams);
    assertTrue(forwarder.getArgs().isEmpty());
  }

  @Test
  void forwardsUnassignedShardWhileConsumingAssigned() throws Exception {
    var svcA = "svcA";
    var svcB = "svcB";
    var streamIds = List.of(0, 1);
    var assignedShards = List.of(0);
    var walShards = List.of(0, 1);
    var streamShardMap = Map.of(svcA, 0, svcB, 1);
    LogsCfg cfg = new TestLogsConfig(tempDir.resolve("logs-data"));

    var injector =
        buildInjector(
            cfg,
            tempDir.resolve("wal"),
            Path.of(cfg.getDataDir()),
            assignedShards,
            streamIds,
            streamShardMap);

    bootstrapShardState(injector, assignedShards, walShards);

    var ingester = injector.getInstance(LogsIngester.class);
    var driver = injector.getInstance(LogsConsumerDriver.class);
    var bufferPool = injector.getInstance(LoggingLogsBufferPool.class);
    var forwarder = injector.getInstance(FakeLogForwarder.class);

    ingester.ingest(
        buildRequest(
            Map.of(
                svcA, List.of(buildLogRecord("msg1", 1_000_000_000L)),
                svcB, List.of(buildLogRecord("msg2", 61_000_000_000L)))));
    driver.onTick();

    assertEquals(1, bufferPool.getCommands().size());
    assertEquals("0", bufferPool.getCommands().get(0).streamId());
    assertEquals(1, forwarder.getArgs().size());
    assertEquals(1, forwarder.getArgs().get(0).records().getShard());
    assertEquals(1, forwarder.getArgs().get(0).records().getRecords().size());
  }

  private Injector buildInjector(
      LogsCfg logsCfg,
      Path walRoot,
      Path logsDataDir,
      List<Integer> assignedShards,
      List<Integer> streamIds,
      Map<String, Integer> streamShardMap) {
    Map<Long, Integer> blockShardMap = Map.of();
    var shardingModule =
        new TestLogShardingModule(
            TestWhoAmiFactory.defaultTestWhoAmi(),
            assignedShards,
            blockShardMap,
            streamShardMap,
            REMOTE_NODE_ID);
    var walModule = new TestLogsWalModule(walRoot, streamIds);
    var logsModule = new TestLogsModule(logsCfg, logsDataDir, 8);
    return Guice.createInjector(shardingModule, walModule, logsModule);
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

  private ExportLogsServiceRequest buildRequest(Map<String, List<LogRecord>> recordsBySvc) {
    var builder = ExportLogsServiceRequest.newBuilder();
    for (var entry : recordsBySvc.entrySet()) {
      var svc = entry.getKey();
      builder.addResourceLogs(
          ResourceLogs.newBuilder()
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
              .addScopeLogs(ScopeLogs.newBuilder().addAllLogRecords(entry.getValue())));
    }
    return builder.build();
  }

  private LogRecord buildLogRecord(String msg, long tsNanos) {
    return LogRecord.newBuilder()
        .setTimeUnixNano(tsNanos)
        .setBody(
            io.opentelemetry.proto.common.v1.AnyValue.newBuilder()
                .setBytesValue(
                    com.google.protobuf.ByteString.copyFrom(
                        ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8))))
                .build())
        .build();
  }
}

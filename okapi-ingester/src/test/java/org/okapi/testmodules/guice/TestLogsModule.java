/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.testmodules.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.ShardToStringFlyweights;
import org.okapi.abstractio.StreamIdFactory;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.identity.MemberList;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.forwarding.LogForwarder;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageBody;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.io.LogPageNonChecksummedCodec;
import org.okapi.logs.io.LogPageSnapshot;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.logs.service.LogsConsumerDriver;
import org.okapi.logs.service.LogsIngester;
import org.okapi.logs.service.LogsShardWalConsumer;
import org.okapi.pages.Codec;
import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardRegistry;
import org.okapi.testmodules.FakeLogForwarder;
import org.okapi.testmodules.LoggingLogsBufferPool;
import org.okapi.wal.consumer.WalConsumerController;

public class TestLogsModule extends AbstractModule {
  private final LogsCfg logsCfg;
  private final Path logsDataDir;
  private final int batchSize;

  public TestLogsModule(LogsCfg logsCfg, Path logsDataDir, int batchSize) {
    this.logsCfg = logsCfg;
    this.logsDataDir = logsDataDir;
    this.batchSize = batchSize;
  }

  @Override
  protected void configure() {
    bind(LogsCfg.class).toInstance(logsCfg);
    bind(FakeLogForwarder.class).in(Singleton.class);
    bind(LogForwarder.class).to(FakeLogForwarder.class);
    bind(LoggingLogsBufferPool.class).in(Singleton.class);
    bind(LogsBufferPool.class).to(LoggingLogsBufferPool.class);
  }

  @Provides
  @Singleton
  MeterRegistry provideMeterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Provides
  @Singleton
  DiskLogBinPaths<String> provideLogsDiskPaths() throws IOException {
    Files.createDirectories(logsDataDir);
    return new LogsDiskPaths(logsDataDir, logsCfg.getIdxExpiryDuration(), "logs");
  }

  @Provides
  @Singleton
  Codec<LogPage, LogPageSnapshot, LogPageMetadata, LogPageBody> provideLogCodec() {
    return new LogPageNonChecksummedCodec();
  }

  @Provides
  @Singleton
  LogsIngester provideLogsIngester(
      LogsCfg cfg,
      WalResourcesPerStream<Integer> walResourcesPerStream,
      ShardAssigner<String> shardAssigner) {
    return new LogsIngester(cfg, walResourcesPerStream, shardAssigner);
  }

  @Provides
  @Singleton
  LogsShardWalConsumer provideLogsShardWalConsumer(
      WalResourcesPerStream<Integer> walResourcesPerStream,
      LogsBufferPool logsBufferPool,
      LogForwarder logForwarder,
      MemberList memberList,
      StreamIdFactory factory,
      ShardRegistry shardRegistry) {
    return new LogsShardWalConsumer(
        walResourcesPerStream,
        logsBufferPool,
        batchSize,
        factory,
        logForwarder,
        memberList,
        shardRegistry);
  }

  @Provides
  @Singleton
  WalConsumerController provideWalConsumerController() {
    var controller = new WalConsumerController();
    controller.start();
    return controller;
  }

  @Provides
  @Singleton
  LogsConsumerDriver provideLogsWalConsumerDriver(
      LogsShardWalConsumer logsShardWalConsumer, WalConsumerController walConsumerController) {
    return new LogsConsumerDriver(logsShardWalConsumer, walConsumerController);
  }

  @Provides
  @Singleton
  StreamIdFactory provideStreamIdFactory(ShardToStringFlyweights shardToStringFlyweights) {
    return new StreamIdFactory(shardToStringFlyweights);
  }

  @Provides
  @Singleton
  ShardToStringFlyweights shardToStringFlyweights() {
    return new ShardToStringFlyweights();
  }
}

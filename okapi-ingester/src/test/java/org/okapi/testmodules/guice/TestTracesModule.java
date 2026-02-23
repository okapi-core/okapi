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
import org.okapi.pages.Codec;
import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardRegistry;
import org.okapi.testmodules.FakeTraceForwarder;
import org.okapi.testmodules.LoggingTracesBufferPool;
import org.okapi.traces.TracesBufferPool;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.SpanPage;
import org.okapi.traces.io.SpanPageBody;
import org.okapi.traces.io.SpanPageCodec;
import org.okapi.traces.io.SpanPageMetadata;
import org.okapi.traces.io.SpanPageSnapshot;
import org.okapi.traces.paths.TracesDiskPaths;
import org.okapi.traces.service.HttpTraceForwarder;
import org.okapi.traces.service.TracesConsumerDriver;
import org.okapi.traces.service.TracesIngester;
import org.okapi.traces.service.TracesShardWalConsumer;
import org.okapi.wal.consumer.WalConsumerController;

public class TestTracesModule extends AbstractModule {
  private final TracesCfg cfg;
  private final Path tracesDataDir;
  private final int batchSize;

  public TestTracesModule(TracesCfg cfg, Path tracesDataDir, int batchSize) {
    this.cfg = cfg;
    this.tracesDataDir = tracesDataDir;
    this.batchSize = batchSize;
  }

  @Override
  protected void configure() {
    bind(TracesCfg.class).toInstance(cfg);
    bind(FakeTraceForwarder.class).in(Singleton.class);
    bind(HttpTraceForwarder.class).to(FakeTraceForwarder.class);
    bind(LoggingTracesBufferPool.class).in(Singleton.class);
    bind(TracesBufferPool.class).to(LoggingTracesBufferPool.class);
  }

  @Provides
  @Singleton
  MeterRegistry provideMeterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Provides
  @Singleton
  DiskLogBinPaths<String> provideTracesDiskPaths() throws IOException {
    Files.createDirectories(tracesDataDir);
    return new TracesDiskPaths(tracesDataDir, cfg.getIdxExpiryDuration(), "traces");
  }

  @Provides
  @Singleton
  Codec<SpanPage, SpanPageSnapshot, SpanPageMetadata, SpanPageBody> provideSpanCodec() {
    return new SpanPageCodec();
  }

  @Provides
  @Singleton
  TracesIngester provideTracesIngester(
      TracesCfg cfg,
      WalResourcesPerStream<Integer> walResourcesPerStream,
      ShardAssigner<String> shardAssigner) {
    return new TracesIngester(cfg, walResourcesPerStream, shardAssigner);
  }

  @Provides
  @Singleton
  TracesShardWalConsumer provideTracesShardWalConsumer(
      WalResourcesPerStream<Integer> walResourcesPerStream,
      TracesBufferPool tracesBufferPool,
      StreamIdFactory streamIdFactory,
      HttpTraceForwarder forwarder,
      MemberList memberList,
      ShardRegistry shardRegistry) {
    return new TracesShardWalConsumer(
        walResourcesPerStream,
        tracesBufferPool,
        batchSize,
        streamIdFactory,
        forwarder,
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
  TracesConsumerDriver provideTracesConsumerDriver(
      TracesShardWalConsumer tracesShardWalConsumer, WalConsumerController walConsumerController) {
    return new TracesConsumerDriver(tracesShardWalConsumer, walConsumerController);
  }

  @Provides
  @Singleton
  StreamIdFactory provideStreamIdFactory() {
    try {
      var factory = new StreamIdFactory(null);
      var field = StreamIdFactory.class.getDeclaredField("shardToStringFlyweights");
      field.setAccessible(true);
      field.set(factory, new ShardToStringFlyweights());
      return factory;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

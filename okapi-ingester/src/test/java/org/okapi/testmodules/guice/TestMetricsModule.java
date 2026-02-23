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
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.identity.MemberList;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.metrics.otel.OtelConverter;
import org.okapi.metrics.paths.MetricsDiskPaths;
import org.okapi.metrics.query.PayloadSplitter;
import org.okapi.metrics.service.FakeMetricsForwarder;
import org.okapi.metrics.service.MetricsForwarder;
import org.okapi.metrics.service.MetricsGrouper;
import org.okapi.metrics.service.MetricsGrouperImpl;
import org.okapi.metrics.service.MetricsIngester;
import org.okapi.metrics.service.MetricsShardWalConsumer;
import org.okapi.metrics.service.MetricsStreamIdFactory;
import org.okapi.metrics.service.MetricsWalConsumerDriver;
import org.okapi.pages.MetricsBufferPool;
import org.okapi.sharding.ShardAssigner;
import org.okapi.sharding.ShardRegistry;
import org.okapi.testmodules.LoggingMetricsBufferPool;
import org.okapi.wal.consumer.WalConsumerController;

public class TestMetricsModule extends AbstractModule {
  private final MetricsCfg metricsCfg;
  private final Path metricsDataDir;
  private final int batchSize;

  public TestMetricsModule(MetricsCfg metricsCfg, Path metricsDataDir, int batchSize) {
    this.metricsCfg = metricsCfg;
    this.metricsDataDir = metricsDataDir;
    this.batchSize = batchSize;
  }

  @Override
  protected void configure() {
    bind(MetricsCfg.class).toInstance(metricsCfg);
    bind(FakeMetricsForwarder.class).in(Singleton.class);
    bind(MetricsForwarder.class).to(FakeMetricsForwarder.class);
    bind(LoggingMetricsBufferPool.class).in(Singleton.class);
    bind(MetricsBufferPool.class).to(LoggingMetricsBufferPool.class);
  }

  @Provides
  @Singleton
  MeterRegistry provideMeterRegistry() {
    return new SimpleMeterRegistry();
  }

  @Provides
  @Singleton
  DiskLogBinPaths<String> provideMetricsDiskPaths() throws IOException {
    Files.createDirectories(metricsDataDir);
    return new MetricsDiskPaths(metricsDataDir, metricsCfg.getIdxExpiryDuration(), "metrics");
  }

  @Provides
  @Singleton
  MetricsStreamIdFactory provideMetricsStreamIdFactory() {
    return new MetricsStreamIdFactory();
  }

  @Provides
  @Singleton
  PayloadSplitter providePayloadSplitter(MetricsCfg cfg) {
    return new PayloadSplitter(cfg);
  }

  @Provides
  @Singleton
  MetricsGrouper provideMetricsGrouper(
      PayloadSplitter payloadSplitter, ShardAssigner<String> shardAssigner) {
    return new MetricsGrouperImpl(payloadSplitter, shardAssigner, new MetricsStreamIdFactory());
  }

  @Provides
  @Singleton
  OtelConverter provideOtelConverter() {
    return new OtelConverter();
  }

  @Provides
  @Singleton
  MetricsIngester provideMetricsIngester(
      OtelConverter otelConverter,
      WalResourcesPerStream<Integer> walResourcesPerStream,
      MetricsGrouper metricsGrouper) {
    return new MetricsIngester(otelConverter, walResourcesPerStream, metricsGrouper);
  }

  @Provides
  @Singleton
  MetricsShardWalConsumer provideMetricsShardWalConsumer(
      WalResourcesPerStream<Integer> walResourcesPerStream,
      MetricsBufferPool metricsBufferPool,
      MetricsStreamIdFactory metricsStreamIdFactory,
      MetricsForwarder metricsForwarder,
      MemberList memberList,
      ShardRegistry shardRegistry) {
    return new MetricsShardWalConsumer(
        walResourcesPerStream,
        metricsBufferPool,
        batchSize,
        metricsStreamIdFactory,
        metricsForwarder,
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
  MetricsWalConsumerDriver provideMetricsWalConsumerDriver(
      MetricsShardWalConsumer metricsShardWalConsumer,
      WalConsumerController walConsumerController) {
    return new MetricsWalConsumerDriver(metricsShardWalConsumer, walConsumerController);
  }
}

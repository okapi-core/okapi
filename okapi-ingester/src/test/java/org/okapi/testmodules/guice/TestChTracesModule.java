/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.testmodules.guice;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.enums.Protocol;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.metrics.ch.ChWriter;
import org.okapi.traces.ch.ChSpanAttributeHintsService;
import org.okapi.traces.ch.ChSpanStatsQueryService;
import org.okapi.traces.ch.ChTraceQueryService;
import org.okapi.traces.ch.ChTracesIngester;
import org.okapi.traces.ch.ChTracesWalConsumer;
import org.okapi.traces.ch.ChTracesWalConsumerDriver;
import org.okapi.traces.ch.NoopSpanFilterStrategy;
import org.okapi.traces.ch.NoopTraceFilterStrategy;
import org.okapi.traces.ch.OtelTracesToChRowsConverter;
import org.okapi.traces.ch.SpanFilterStrategy;
import org.okapi.traces.ch.TraceFilterStrategy;
import org.okapi.traces.ch.template.ChTraceTemplateEngine;
import org.okapi.wal.io.WalReader;
import org.okapi.wal.manager.WalManager;

public class TestChTracesModule extends AbstractModule {
  private final Path walDir;
  private final int batchSize;

  public TestChTracesModule(Path walDir, int batchSize) {
    this.walDir = walDir;
    this.batchSize = batchSize;
  }

  @Provides
  @Singleton
  Client provideClient() {
    return getChClient();
  }

  @Provides
  @Singleton
  WalManager.WalConfig provideWalConfig() {
    return new WalManager.WalConfig(1_048_576);
  }

  @Provides
  @Singleton
  ChWalResources provideWalResources(WalManager.WalConfig walConfig) throws IOException {
    Files.createDirectories(walDir);
    return new ChWalResources(walDir, walConfig);
  }

  @Provides
  @Singleton
  ChWriter provideChWriter(Client client) {
    return new ChWriter(client);
  }

  @Provides
  @Singleton
  TraceFilterStrategy provideTraceFilterStrategy() {
    return new NoopTraceFilterStrategy();
  }

  @Provides
  @Singleton
  SpanFilterStrategy provideSpanFilterStrategy() {
    return new NoopSpanFilterStrategy();
  }

  @Provides
  @Singleton
  ChTracesIngester provideChTracesIngester(
      ChWalResources walResources,
      TraceFilterStrategy traceFilterStrategy,
      SpanFilterStrategy spanFilterStrategy) {
    return new ChTracesIngester(walResources, traceFilterStrategy, spanFilterStrategy);
  }

  @Provides
  @Singleton
  OtelTracesToChRowsConverter provideOtelTracesToChRowsConverter() {
    return new OtelTracesToChRowsConverter();
  }

  @Provides
  @Singleton
  ChTracesWalConsumer provideChTracesWalConsumer(
      ChWalResources walResources, ChWriter writer, OtelTracesToChRowsConverter converter) {
    WalReader reader = walResources.getReader();
    return new ChTracesWalConsumer(walResources, batchSize, writer, converter);
  }

  @Provides
  @Singleton
  ChTracesWalConsumerDriver provideChTracesWalConsumerDriver(ChTracesWalConsumer consumer) {
    return new ChTracesWalConsumerDriver(consumer);
  }

  @Provides
  @Singleton
  ChTraceTemplateEngine provideTraceTemplateEngine() {
    return new ChTraceTemplateEngine();
  }

  @Provides
  @Singleton
  ChTraceQueryService provideChTraceQueryService(
      Client client, ChTraceTemplateEngine templateEngine) {
    return new ChTraceQueryService(client, templateEngine);
  }

  @Provides
  @Singleton
  ChSpanStatsQueryService provideChSpanStatsQueryService(
      Client client, ChTraceTemplateEngine templateEngine) {
    return new ChSpanStatsQueryService(client, templateEngine);
  }

  @Provides
  @Singleton
  ChSpanAttributeHintsService provideChSpanAttributeHintsService(
      Client client, ChTraceTemplateEngine templateEngine) {
    return new ChSpanAttributeHintsService(client, templateEngine);
  }

  private Client getChClient() {
    return new Client.Builder()
        .addEndpoint(Protocol.HTTP, "localhost", 8123, false)
        .setUsername("default")
        .setPassword("okapi_testing_password")
        .build();
  }
}

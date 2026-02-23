/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.ch;

import java.util.List;
import org.okapi.metrics.ch.ChMetricsQueryProcessor;
import org.okapi.metrics.ch.ChMetricsWalConsumer;
import org.okapi.metrics.ch.ChMetricsWalConsumerDriver;
import org.okapi.metrics.ch.ChWalResources;
import org.okapi.metrics.ch.ChWriter;
import org.okapi.metrics.ch.template.ChMetricTemplateEngine;
import org.okapi.runtime.ch.ChWalConsumerCommonDriver;
import org.okapi.spring.configs.Qualifiers;
import org.okapi.spring.configs.properties.ChWalConsumerCfg;
import org.okapi.traces.ch.ChTracesWalConsumer;
import org.okapi.traces.ch.ChTracesWalConsumerDriver;
import org.okapi.traces.ch.OtelTracesToChRowsConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChWalConsumersConfig {

  @Bean
  public ChWriter chWriter(@Autowired com.clickhouse.client.api.Client client) {
    return new ChWriter(client);
  }

  @Bean
  public ChMetricsWalConsumer chMetricsWalConsumer(
      @Autowired @Qualifier(Qualifiers.METRICS_CH_WAL_RESOURCES) ChWalResources walResources,
      @Autowired ChWriter writer,
      @Autowired ChWalConsumerCfg walCfg) {
    return new ChMetricsWalConsumer(walCfg.getBatchSize(), writer, walResources);
  }

  @Bean
  public ChMetricsWalConsumerDriver chMetricsWalConsumerDriver(
      @Autowired ChMetricsWalConsumer walConsumer) {
    return new ChMetricsWalConsumerDriver(walConsumer);
  }

  @Bean
  public ChWalConsumerCommonDriver chWalConsumerCommonDriver(
      @Autowired ChMetricsWalConsumerDriver metricsDriver,
      @Autowired ChTracesWalConsumerDriver tracesDriver) {
    return new ChWalConsumerCommonDriver(List.of(metricsDriver, tracesDriver));
  }

  @Bean
  public OtelTracesToChRowsConverter otelTracesToChRowsConverter() {
    return new OtelTracesToChRowsConverter();
  }

  @Bean
  public ChTracesWalConsumer chTracesWalConsumer(
      @Autowired @Qualifier(Qualifiers.TRACES_CH_WAL_RESOURCES) ChWalResources walResources,
      @Autowired ChWriter writer,
      @Autowired ChWalConsumerCfg walCfg,
      @Autowired OtelTracesToChRowsConverter converter) {
    return new ChTracesWalConsumer(walResources, walCfg.getBatchSize(), writer, converter);
  }

  @Bean
  public ChTracesWalConsumerDriver chTracesWalConsumerDriver(
      @Autowired ChTracesWalConsumer walConsumer) {
    return new ChTracesWalConsumerDriver(walConsumer);
  }

  @Bean
  public ChMetricsQueryProcessor chMetricsQueryProcessor(
      @Autowired com.clickhouse.client.api.Client client,
      @Autowired ChMetricTemplateEngine templateEngine) {
    return new ChMetricsQueryProcessor(client, templateEngine);
  }
}

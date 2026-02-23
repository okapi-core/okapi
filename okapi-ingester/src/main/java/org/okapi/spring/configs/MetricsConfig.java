package org.okapi.spring.configs;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.okapi.metrics.io.MetricsPageCodec;
import org.okapi.spring.configs.properties.QueryCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class MetricsConfig {

  @Bean
  public MetricsPageCodec metricsPageCodec() {
    return new MetricsPageCodec();
  }

  @Bean(name = Qualifiers.EXEC_PEER_METRICS)
  public ExecutorService executorService(@Autowired QueryCfg queryCfg) {
    return Executors.newFixedThreadPool(queryCfg.getMetricsFanoutPoolSize());
  }

  @Bean(name = Qualifiers.EXEC_METRICS_MULTI_SOURCE)
  public ExecutorService metricsMultiSourceExec(@Autowired QueryCfg queryCfg) {
    return Executors.newFixedThreadPool(queryCfg.getMetricsQueryProcPoolSize());
  }

  @Bean(name = Qualifiers.METRICS_PEER_QUERY_TIMEOUT)
  public Duration metricsPeerQueryTimeout(@Autowired QueryCfg queryCfg) {
    return Duration.ofMillis(queryCfg.getMetricsFanoutTimeoutMs());
  }
}

package org.okapi.logs.spring;

import io.micrometer.core.instrument.MeterRegistry;
import org.okapi.logs.stats.NoOpStatsEmitter;
import org.okapi.logs.stats.OtelStatsEmitter;
import org.okapi.logs.stats.StatsEmitter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatsConfiguration {
  @Bean
  public StatsEmitter statsEmitter(ObjectProvider<MeterRegistry> mrProvider) {
    MeterRegistry mr = mrProvider.getIfAvailable();
    if (mr != null) return new OtelStatsEmitter(mr);
    return new NoOpStatsEmitter();
  }
}


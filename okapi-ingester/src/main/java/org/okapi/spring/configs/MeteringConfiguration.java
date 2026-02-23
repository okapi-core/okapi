/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MeteringConfiguration {

  @Bean
  @ConditionalOnMissingBean(MeterRegistry.class)
  public MeterRegistry meterRegistry() {
    return new SimpleMeterRegistry();
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs;

import java.time.Duration;
import org.okapi.logs.config.LogsCfg;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@Configuration
public class LogsConfiguration {
  @Bean(name = Qualifiers.DISK_QP_MAX_QUERY_WIN)
  public Duration getDiskQpMaxQueryWin(LogsCfg cfg) {
    return Duration.ofMillis(6 * cfg.getIdxExpiryDuration());
  }
}

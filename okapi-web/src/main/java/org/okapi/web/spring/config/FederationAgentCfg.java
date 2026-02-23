/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.spring.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "okapi.federation.agent")
@Validated
public class FederationAgentCfg {
  int maxJobsPerDispatch = 10;
}

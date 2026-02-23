/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.spring.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "okapi.aws")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AwsConfigurationValues {
  String region;
  String endpoint;
  String creds;
}

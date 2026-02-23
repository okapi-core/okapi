/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.spring.configs.properties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.okapi.spring.configs.Profiles;
import org.okapi.traces.config.TracesCfg;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
@ConfigurationProperties(prefix = "okapi.traces")
@Component
@Validated
public class TracesCfgImpl extends AbstractBaseTelemetryConfig implements TracesCfg {}

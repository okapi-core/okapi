/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ServiceFilter {
  @JsonPropertyDescription("Name of the service that emitted the span.")
  String service;

  @JsonPropertyDescription("Name of the downstream peer service called by the span.")
  String peer;
}

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
  @JsonPropertyDescription("""
  Name of the service that emitted the span.
  Use this to find traces related to a specific service.
  """)
  String service;

  @JsonPropertyDescription("""
  Name of the downstream peer service called by the span.
  This need NOT always be set. This field can be set when trying to find spans for communication between two services.
  """)
  String peer;
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class HttpFilters {
  @JsonPropertyDescription("HTTP request method to filter by (e.g. GET, POST).")
  String httpMethod;

  @JsonPropertyDescription("HTTP response status code to filter by (e.g. 200, 500).")
  Integer statusCode;

  @JsonPropertyDescription("HTTP request origin header value to filter by.")
  String origin;

  @JsonPropertyDescription(
      "HTTP target host to filter by. Maps to the http.host or server.address span attribute.")
  String host;
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@JsonClassDescription("Sum/counter query results.")
@ToString
public class GetSumsResponse {
  @JsonPropertyDescription("List of sum intervals covering the queried time range.")
  List<Sum> sums;
}

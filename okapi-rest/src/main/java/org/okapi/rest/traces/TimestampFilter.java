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
public class TimestampFilter {
  @JsonPropertyDescription(
      "Query window start time, in nanoseconds since Unix epoch (inclusive).")
  long tsStartNanos;

  @JsonPropertyDescription(
      "Query window end time, in nanoseconds since Unix epoch (exclusive).")
  long tsEndNanos;
}

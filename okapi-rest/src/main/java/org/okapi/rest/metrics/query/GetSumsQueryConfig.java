/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class GetSumsQueryConfig {
  public enum TEMPORALITY {
    DELTA_AGGREGATE,
    CUMULATIVE
  }

  @JsonPropertyDescription(
      "Sum temporality: DELTA_AGGREGATE (sum of deltas over the window) or CUMULATIVE (latest cumulative value).")
  TEMPORALITY temporality;
}

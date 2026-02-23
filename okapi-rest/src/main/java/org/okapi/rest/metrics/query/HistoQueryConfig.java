/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class HistoQueryConfig {
  public enum TEMPORALITY {
    CUMULATIVE,
    DELTA,
    MERGED
  }

  TEMPORALITY temporality;
}

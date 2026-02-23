/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class GaugeQueryConfig {
  RES_TYPE resolution;
  AGG_TYPE aggregation;
}

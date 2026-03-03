/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.query;

import java.util.List;
import lombok.*;
import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@ToString
public class GetGaugeResponse {
  RES_TYPE resolution;
  AGG_TYPE aggregation;
  List<GaugeSeries> series;
}

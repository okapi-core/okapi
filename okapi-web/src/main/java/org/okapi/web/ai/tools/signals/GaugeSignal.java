/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.signals;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.okapi.web.ai.tools.ResourcePath;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;

@Getter
public class GaugeSignal {
  TimeSeriesQuery timeSeriesQuery;
  ResourcePath resourcePath;
  List<Long> timestamps;
  List<Double> values;
  String unit;

  @Builder
  public GaugeSignal(@Singular List<Long> timestamps, @Singular List<Double> values) {
    this.timestamps = timestamps;
    this.values = values;
  }
}

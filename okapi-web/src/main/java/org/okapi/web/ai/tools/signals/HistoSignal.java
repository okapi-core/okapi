/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools.signals;

import java.util.List;
import lombok.Getter;
import lombok.Singular;
import org.okapi.web.ai.tools.ResourcePath;
import org.okapi.web.ai.tools.params.TimeSeriesQuery;

@Getter
public class HistoSignal {
  TimeSeriesQuery timeSeriesQuery;
  ResourcePath resourcePath;
  List<Long> counts;
  List<Double> buckets;

  public HistoSignal(
      TimeSeriesQuery timeSeriesQuery,
      ResourcePath resourcePath,
      @Singular List<Long> counts,
      @Singular List<Double> buckets) {
    this.timeSeriesQuery = timeSeriesQuery;
    this.resourcePath = resourcePath;
    this.counts = counts;
    this.buckets = buckets;
  }
}

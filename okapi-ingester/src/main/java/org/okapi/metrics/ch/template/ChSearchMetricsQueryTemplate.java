package org.okapi.metrics.ch.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChSearchMetricsQueryTemplate {
  String table;
  long startMs;
  long endMs;
}

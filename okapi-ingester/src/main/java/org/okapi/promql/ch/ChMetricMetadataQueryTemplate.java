package org.okapi.promql.ch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChMetricMetadataQueryTemplate {
  String table;
  String metric;
  long startMs;
  long endMs;
  int limit;
}

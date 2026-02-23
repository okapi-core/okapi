package org.okapi.metrics.ch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class MetricHintsQueryTemplate {
  String table;
  String eventType;
  String svc;
  String svcPrefix;
  String metricPrefix;
  String metric;
  String tagPrefix;
  String tag;
  Map<String, String> otherTags;
  long startMs;
  long endMs;
  int limit;
}

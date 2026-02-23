package org.okapi.promql.ch;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChGetGaugeRawQueryTemplate {
  String table;
  String resource;
  String metric;
  long startMs;
  long endMs;
  Map<String, String> tags;
}

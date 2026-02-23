package org.okapi.metrics.ch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChGetGaugeQueryTemplate {
  String table;
  String bucketExpr;
  String aggExpr;
  String resource;
  String metric;
  long startMs;
  long endMs;
  Map<String, String> tags;
}

package org.okapi.metrics.ch;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChHistoSampleInsertRow {
  String resource;
  String metric_name;
  Map<String, String> tags;
  long ts_start_ms;
  long ts_end_ms;
  float[] buckets;
  int[] counts;
  String histo_type;
}

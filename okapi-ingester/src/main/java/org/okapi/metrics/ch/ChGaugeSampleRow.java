package org.okapi.metrics.ch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChGaugeSampleRow {
  long timestamp;
  String resource;
  String metric;
  Map<String, String> tags;
  float value;
}

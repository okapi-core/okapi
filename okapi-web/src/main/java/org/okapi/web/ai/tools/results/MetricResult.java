package org.okapi.web.ai.tools.results;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor
@EqualsAndHashCode
@Getter
public abstract class MetricResult {
  String path;
  Map<String, String> tags;
  String unit;
}

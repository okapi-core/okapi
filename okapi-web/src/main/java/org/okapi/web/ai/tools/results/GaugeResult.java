package org.okapi.web.ai.tools.results;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class GaugeResult extends MetricResult {
  @Builder
  public GaugeResult(
      String path,
      Map<String, String> tags,
      String unit,
      List<Double> values,
      List<Long> timestamps) {
    super(path, tags, unit);
    this.values = values;
    this.timestamps = timestamps;
  }

  List<Double> values;
  List<Long> timestamps;
}

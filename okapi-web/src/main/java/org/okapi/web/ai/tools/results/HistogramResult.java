package org.okapi.web.ai.tools.results;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode(callSuper = true)
public class HistogramResult extends MetricResult {
  List<Double> buckets;
  List<Long> counts;

  @Builder
  public HistogramResult(
      String path, Map<String, String> tags, String unit, List<Double> buckets, List<Long> counts) {
    super(path, tags, unit);
    this.buckets = buckets;
    this.counts = counts;
  }
}

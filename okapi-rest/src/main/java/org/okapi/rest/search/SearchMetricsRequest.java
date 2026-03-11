package org.okapi.rest.search;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@JsonClassDescription("A SearchMetricsRequest object is used to encapsulate search parameters when searching for metrics.")
@ToString
public class SearchMetricsRequest {
  @JsonPropertyDescription(
      "Start time for time window. This should be linux epoch time. "
          + "We'll only return metric paths if a metric was emitted with timestamp >= tsStartMillis")
  long tsStartMillis;

  @JsonPropertyDescription(
      "End time for time window. This should be linux epoch time."
          + "We'll only return metric paths if a metric was emitted with timestamp <= tsEndMillis")
  long tsEndMillis;

  @JsonPropertyDescription(
      "Metric pattern to match. This should be a valid RE2 regular expression.")
  String metricNamePattern;

  @JsonPropertyDescription(
      "Metric name. This will be matched exactly against the metric name. Matching is case sensitive.")
  String metricName;

  @JsonPropertyDescription(
      "List label-value filters to match. Filters will are matched as an AND of each filter.")
  List<LabelValueFilter> valueFilters;

  @JsonPropertyDescription(
      "List label-value pattern filters to match. Filters will are matched as an AND of each filter.")
  List<LabelValuePatternFilter> patternFilters;

  @JsonPropertyDescription(
      "Optional cross-field filter. If set, the metric name or any tag value must match the"
          + " filter's value (exact) or pattern (RE2). Applied before all other filters.")
  AnyMetricOrValueFilter anyMetricOrValueFilter;
}

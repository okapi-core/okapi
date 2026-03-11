package org.okapi.rest.search;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class AnyMetricOrValueFilter {

  @JsonPropertyDescription(
      "Exact value to match. Either the metric name or any tag value in the metric path must equal"
          + " this string exactly. Takes precedence over pattern if both are set.")
  String value;

  @JsonPropertyDescription(
      "RE2 regular expression. Either the metric name or any tag value in the metric path must"
          + " fully match this pattern. Ignored when value is set.")
  String pattern;
}

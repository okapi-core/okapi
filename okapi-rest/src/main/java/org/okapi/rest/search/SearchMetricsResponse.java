package org.okapi.rest.search;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.*;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@ToString
@Builder
@JsonClassDescription("The set of metric paths matching all supplied search filters.")
public class SearchMetricsResponse {
  @JsonPropertyDescription("List of metric paths (name + label set) that matched all filters.")
  List<MetricPath> matchingPaths;
}

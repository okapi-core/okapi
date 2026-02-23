package org.okapi.rest.search;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@Builder
@NoArgsConstructor
public class SearchResourcesRequest {
  long start;
  long end;
  @Valid MetricEventFilter metricEventFilter;
}

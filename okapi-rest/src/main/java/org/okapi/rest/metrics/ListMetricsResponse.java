package org.okapi.rest.metrics;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.List;

@Builder
@Value
@Getter
public class ListMetricsResponse {
  List<MetricsPathSpecifier> results;
}

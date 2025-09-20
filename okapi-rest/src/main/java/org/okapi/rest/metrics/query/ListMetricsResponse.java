package org.okapi.rest.metrics.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.okapi.rest.metrics.MetricsPathSpecifier;

import java.util.List;

@Builder
@Value
@Getter
public class ListMetricsResponse {
  List<MetricsPathSpecifier> results;
}

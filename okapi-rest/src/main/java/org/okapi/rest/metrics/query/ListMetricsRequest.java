package org.okapi.rest.metrics.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
@Getter
public class ListMetricsRequest {
  String tenantId;
  String app;
  long start;
  long end;
}

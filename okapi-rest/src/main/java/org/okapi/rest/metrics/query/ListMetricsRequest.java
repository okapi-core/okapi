package org.okapi.rest.metrics.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Builder
@Value
@Getter
public class ListMetricsRequest {
  String app;
  String tenantId;
  long start;
  long end;
}

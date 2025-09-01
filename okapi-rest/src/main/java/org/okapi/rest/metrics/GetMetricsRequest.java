package org.okapi.rest.metrics;

import org.okapi.metrics.pojos.AGG_TYPE;
import org.okapi.metrics.pojos.RES_TYPE;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Builder
@Getter
public class GetMetricsRequest {

  @NotNull(message = "metric must be specified.")
  String metricName;

  @NotNull(message = "team must be specified.")
  String team;

  Map<String, String> tags;

  @NotNull
  @NotNull(message = "start of interval must be specified.")
  long start;

  @NotNull
  @NotNull(message = "end of interval must be specified.")
  long end;

  @NotNull
  @NotNull(message = "resolution must be specified.")
  RES_TYPE resolution;

  @NotNull
  @NotNull(message = "aggregation type must be specified.")
  AGG_TYPE aggregation;
}

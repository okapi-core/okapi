package org.okapi.rest.traces.red;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.okapi.metrics.pojos.RES_TYPE;
import org.okapi.rest.traces.TimestampFilter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ServiceRedRequest {
  // RED for that specific service
  // RED for each operation
  // edge-RED for each of this service.
  @JsonPropertyDescription(
      "Time window for the RED metrics query, in nanoseconds since Unix epoch.")
  @NotNull(message = "time window must be provided") TimestampFilter timestampFilter;

  @JsonPropertyDescription(
      "Name of the service for which to compute RED metrics (rate, error rate, duration).")
  @NotNull(message = "service must be specified") String service;

  @JsonPropertyDescription(
      "Time bucket resolution for the returned time series: SECONDLY, MINUTELY, or HOURLY.")
  @NotNull(message = "resolution must be provided") RES_TYPE resType;
}

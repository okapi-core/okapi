package org.okapi.rest.traces.red;

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
  @NotNull(message = "time window must be provided") TimestampFilter timestampFilter;
  @NotNull(message = "service must be specified") String service;
  @NotNull(message = "resolution must be provided") RES_TYPE resType;
}

package org.okapi.rest.traces.red;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ServiceEdgeRed {
  @NotNull
  String peerService;
  RedMetrics redMetrics;
}

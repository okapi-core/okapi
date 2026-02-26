package org.okapi.rest.traces.red;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ServiceOpRed {
  String op;
  RedMetrics redMetrics;
}

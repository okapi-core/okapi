package org.okapi.rest.metrics.payloads;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Setter
@NoArgsConstructor
@Getter
public class SumPoint {
  Long start;
  Long end;
  int sum;
}

package org.okapi.rest.metrics.payloads;

import lombok.*;

@AllArgsConstructor
@Setter
@NoArgsConstructor
@Getter
@ToString
public class SumPoint {
  Long start;
  Long end;
  int sum;
}

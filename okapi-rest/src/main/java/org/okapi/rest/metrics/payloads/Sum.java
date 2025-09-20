package org.okapi.rest.metrics.payloads;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Value
@Getter
@Setter
@Builder
public class Sum {
  SumType sumType;
  List<SumPoint> sumPoints;
}

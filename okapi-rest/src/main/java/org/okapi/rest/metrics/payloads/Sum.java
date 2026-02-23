package org.okapi.rest.metrics.payloads;

import java.util.List;
import lombok.*;

@AllArgsConstructor
@Value
@Getter
@Setter
@Builder
public class Sum {
  SUM_TEMPORALITY temporality;
  List<SumPoint> sumPoints;
}

package org.okapi.rest.traces;

import java.util.List;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class AttributeNumericSeries {
  String attribute;
  List<NumericPoint> points;
}

package org.okapi.rest.traces;

import java.util.List;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanAttributeValueHintsResponse {
  String attributeName;
  NumericAttributeSummary numericSummary;
  List<String> values;
}

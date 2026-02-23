package org.okapi.rest.traces;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanAttributeValueHintsRequest {
  TimestampMillisFilter timestampFilter;
  String attributeName;
}

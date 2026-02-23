package org.okapi.rest.traces;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ValueCount {
  String value;
  long count;
}

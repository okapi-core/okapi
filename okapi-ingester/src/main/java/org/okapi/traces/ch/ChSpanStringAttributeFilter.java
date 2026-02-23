package org.okapi.traces.ch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChSpanStringAttributeFilter {
  String key;
  int bucket;
  String value;
}

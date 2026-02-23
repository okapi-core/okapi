package org.okapi.traces.ch;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChSpanAttributeValuesDefaultTemplate {
  String table;
  String attributeName;
  Integer limit;
  Long tsStartNs;
  Long tsEndNs;
}

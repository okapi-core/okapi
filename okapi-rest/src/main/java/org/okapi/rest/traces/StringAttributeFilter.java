package org.okapi.rest.traces;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class StringAttributeFilter {
  String key;
  String value;
}

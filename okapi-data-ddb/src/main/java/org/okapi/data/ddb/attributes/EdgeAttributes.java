package org.okapi.data.ddb.attributes;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Setter
public class EdgeAttributes {
  long timestamp;
  String stringValue;
  boolean booleanValue;
}

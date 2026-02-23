package org.okapi.data.ddb.attributes;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@Setter
public class PanelQueryConfig {
  String localId;
  String query;
  EXPECTED_RESULT_TYPE_DDB expectedResultType;
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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

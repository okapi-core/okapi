/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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

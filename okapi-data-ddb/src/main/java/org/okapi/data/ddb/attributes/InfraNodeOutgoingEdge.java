/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InfraNodeOutgoingEdge {
  InfraEntityId targetNodeId;
  String edgeAttributes;
  DEP_TYPE depType;
}

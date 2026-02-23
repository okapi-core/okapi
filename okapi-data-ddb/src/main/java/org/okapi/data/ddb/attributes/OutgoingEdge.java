/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OutgoingEdge {
  ENTITY_TYPE outgoingNodeType;
  RELATION_TYPE relationType;

  public ENTITY_TYPE outgoingNodeType() {
    return outgoingNodeType;
  }

  public RELATION_TYPE relationType() {
    return relationType;
  }

  public static OutgoingEdge of(ENTITY_TYPE outgoingNodeType, RELATION_TYPE relationType) {
    return new OutgoingEdge(outgoingNodeType, relationType);
  }
}

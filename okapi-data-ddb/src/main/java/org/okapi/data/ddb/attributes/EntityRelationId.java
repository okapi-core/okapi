/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EntityRelationId {
  ENTITY_TYPE entityType;
  String entityId;
  USER_RELATION_TYPE relationType;
}

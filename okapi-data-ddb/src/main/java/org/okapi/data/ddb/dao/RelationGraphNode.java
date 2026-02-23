/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.dao;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.RELATION_TYPE;

@Getter
@Setter
public class RelationGraphNode {
  String entityId;
  String relatedEntity;
  ENTITY_TYPE relatedEntityType;
  List<RELATION_TYPE> relationships;
  Integer version;

  @Builder(toBuilder = true)
  public RelationGraphNode(
      String entityId,
      String relatedEntity,
      ENTITY_TYPE relatedEntityType,
      List<RELATION_TYPE> relationships) {
    this.entityId = entityId;
    this.relatedEntity = relatedEntity;
    this.relatedEntityType = relatedEntityType;
    this.relationships = relationships;
  }
}

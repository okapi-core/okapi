/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.EntityRelationId;
import org.okapi.data.ddb.attributes.USER_RELATION_TYPE;

public class EntityIdSerializer implements CustomSerializer<EntityRelationId> {

  public String serialize(EntityRelationId relationId) {
    // Implement serialization logic here
    return String.join(
        ":",
        relationId.getEntityType().name(),
        relationId.getRelationType().name(),
        relationId.getEntityId());
  }

  public EntityRelationId deserialize(String data) {
    // Implement deserialization logic here
    String[] parts = data.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid data format for EntityRelationId: " + data);
    }
    ENTITY_TYPE entityType = ENTITY_TYPE.valueOf(parts[0]);
    USER_RELATION_TYPE relationType = USER_RELATION_TYPE.valueOf(parts[1]);
    String entityId = parts[2];
    return new EntityRelationId(entityType, entityId, relationType);
  }

  public static final EntityIdSerializer INSTANCE = new EntityIdSerializer();

  public static EntityIdSerializer getSingleton() {
    return INSTANCE;
  }
}

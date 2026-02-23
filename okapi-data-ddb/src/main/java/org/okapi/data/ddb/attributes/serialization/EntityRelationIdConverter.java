package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.EntityRelationId;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class EntityRelationIdConverter extends GenericCustomSerializerConverter<EntityRelationId> {
  @Override
  public CustomSerializer<EntityRelationId> getSerializer() {
    return EntityIdSerializer.getSingleton();
  }

  @Override
  public EnhancedType<EntityRelationId> type() {
    return EnhancedType.of(EntityRelationId.class);
  }
}

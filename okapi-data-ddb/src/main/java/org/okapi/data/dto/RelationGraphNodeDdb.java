package org.okapi.data.dto;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import lombok.*;
import org.okapi.data.ddb.attributes.ENTITY_TYPE;
import org.okapi.data.ddb.attributes.EntityId;
import org.okapi.data.ddb.attributes.RELATION_TYPE;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Builder
@Setter
@NoArgsConstructor
@DynamoDbBean
public class RelationGraphNodeDdb {
  String entityId;
  String relatedEntity;
  List<RELATION_TYPE> relationships;
  ENTITY_TYPE relatedEntityType;

  public RelationGraphNodeDdb(
      String entityId,
      String relatedEntity,
      List<RELATION_TYPE> relationships,
      ENTITY_TYPE relatedEntityType) {
    this.setEntityId(entityId)
        .setRelatedEntity(relatedEntity)
        .setRelationships(relationships)
        .setRelatedEntityType(relatedEntityType);
  }

  public static EntityId makeEntityId(ENTITY_TYPE type, String id) {
    return new EntityId(type, id);
  }

  public static Optional<EntityId> parse(String id) {
    var parts = id.split(":");
    if (parts.length != 2) return Optional.empty();
    else return Optional.of(new EntityId(ENTITY_TYPE.valueOf(parts[0]), parts[1]));
  }

  @DynamoDbAttribute(TableAttributes.EDGE_ID)
  @DynamoDbPartitionKey
  public String getEntityId() {
    return entityId;
  }

  public RelationGraphNodeDdb setEntityId(String entityId) {
    this.entityId = Preconditions.checkNotNull(entityId);
    return this;
  }

  @DynamoDbAttribute(TableAttributes.RELATED_ENTITY)
  @DynamoDbSortKey
  public String getRelatedEntity() {
    return relatedEntity;
  }

  public RelationGraphNodeDdb setRelatedEntity(String relatedEntity) {
    this.relatedEntity = Preconditions.checkNotNull(relatedEntity);
    return this;
  }

  @DynamoDbAttribute(TableAttributes.RELATION_TYPE)
  public List<RELATION_TYPE> getRelationships() {
    return relationships;
  }

  public RelationGraphNodeDdb setRelationships(List<RELATION_TYPE> relationships) {
    this.relationships = Preconditions.checkNotNull(relationships);
    return this;
  }

  @DynamoDbAttribute(TableAttributes.RELATED_ENTITY_TYPE)
  public ENTITY_TYPE getRelatedEntityType() {
    return relatedEntityType;
  }

  public RelationGraphNodeDdb setRelatedEntityType(ENTITY_TYPE relatedEntityType) {
    this.relatedEntityType = Preconditions.checkNotNull(relatedEntityType);
    return this;
  }
}

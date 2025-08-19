package org.okapi.data.dto;

import java.util.List;
import java.util.Optional;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@AllArgsConstructor
@Builder
@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class RelationGraphNodeDdb {
  String entityId;
  String relatedEntity;
  List<RelationGraphNode.RELATION_TYPE> relationType;
  Integer version;

  public RelationGraphNodeDdb setEntityId(String entityId) {
    this.entityId = entityId;
    return this;
  }

  public RelationGraphNodeDdb setRelatedEntity(String relatedEntity) {
    this.relatedEntity = relatedEntity;
    return this;
  }

  public RelationGraphNodeDdb setRelationType(List<RelationGraphNode.RELATION_TYPE> relationType) {
    this.relationType = relationType;
    return this;
  }

  public RelationGraphNodeDdb setVersion(Integer version) {
    this.version = version;
    return this;
  }

  @DynamoDbAttribute(TableAttributes.ENTITY_ID)
  @DynamoDbPartitionKey
  public String getEntityId() {
    return entityId;
  }

  @DynamoDbAttribute(TableAttributes.RELATED_ENTITY)
  @DynamoDbSortKey
  public String getRelatedEntity() {
    return relatedEntity;
  }

  @DynamoDbAttribute(TableAttributes.RELATION_TYPE)
  public List<RelationGraphNode.RELATION_TYPE> getRelationType() {
    return relationType;
  }

  @DynamoDbVersionAttribute
  public Integer getVersion() {
    return version;
  }

  public static String makeEntityId(RelationGraphNode.ENTITY_TYPE type, String id) {
    return type.name() + ":" + id;
  }

  public static Optional<RelationGraphNode.EntityId> parse(String id) {
    var parts = id.split(":");
    if (parts.length != 2) return Optional.empty();
    else return Optional.of(new RelationGraphNode.EntityId(RelationGraphNode.ENTITY_TYPE.valueOf(parts[0]), parts[1]));
  }
}

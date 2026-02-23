package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.ddb.attributes.EdgeAttributes;
import org.okapi.data.ddb.attributes.EntityRelationId;
import org.okapi.data.ddb.attributes.serialization.EdgeAttributeConverter;
import org.okapi.data.ddb.attributes.serialization.EntityRelationIdConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class UserEntityRelations {
  String userId;
  EntityRelationId edgeId;
  EdgeAttributes edgeAttributes;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.USER_ID)
  public String getUserId() {
    return userId;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.EDGE_ID)
  @DynamoDbConvertedBy(EntityRelationIdConverter.class)
  public EntityRelationId getEdgeId() {
    return edgeId;
  }

  @DynamoDbAttribute(TableAttributes.EDGE_ATTRIBUTES)
  @DynamoDbConvertedBy(EdgeAttributeConverter.class)
  public EdgeAttributes getEdgeAttributes() {
    return edgeAttributes;
  }
}

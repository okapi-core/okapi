package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class TokenMetaDdb {
  String orgId;
  String creatorId;
  String tokenId;
  Long createdAt;
  TOKEN_STATUS tokenStatus;

  @DynamoDbPartitionKey
  @DynamoDbSecondaryPartitionKey(indexNames = TablesAndIndexes.TOKEN_META_BY_ORG_STATUS_GSI)
  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  @DynamoDbAttribute(TableAttributes.CREATOR_ID)
  public String getCreatorId() {
    return creatorId;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.TOKEN_ID)
  public String getTokenId() {
    return tokenId;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Long getCreatedAt() {
    return createdAt;
  }

  @DynamoDbAttribute(TableAttributes.TOKEN_STATUS)
  @DynamoDbSecondarySortKey(indexNames = TablesAndIndexes.TOKEN_META_BY_ORG_STATUS_GSI)
  public TOKEN_STATUS getTokenStatus() {
    return tokenStatus;
  }
}

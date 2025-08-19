package org.okapi.data.dto;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
@Builder
public class TeamMemberDtoDdb {
  private String teamId;
  private String userId;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.TEAM_ID)
  public String getTeamId() {
    return teamId;
  }

  public TeamMemberDtoDdb setTeamId(String teamId) {
    this.teamId = teamId;
    return this;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.USER_ID)
  public String getUserId() {
    return userId;
  }

  public TeamMemberDtoDdb setUserId(String userId) {
    this.userId = userId;
    return this;
  }
}

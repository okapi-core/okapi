package org.okapi.data.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationTokenDdb {

  private String authorizationToken; // PK
  private AuthorizationTokenDto.AuthorizationTokenStatus tokenStatus;
  private String orgId;
  private String teamId; // GSI partition key
  private String issuer;
  private Instant created;
  private Long expiryTime;
  private List<String> authorizationRoles;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.AUTHORIZATION_TOKEN_ID)
  public String getAuthorizationToken() {
    return authorizationToken;
  }

  public void setAuthorizationToken(String v) {
    this.authorizationToken = v;
  }

  @DynamoDbAttribute(TableAttributes.AUTHORIZATION_TOKEN_STATUS)
  public AuthorizationTokenDto.AuthorizationTokenStatus getTokenStatus() {
    return tokenStatus;
  }

  public void setTokenStatus(AuthorizationTokenDto.AuthorizationTokenStatus v) {
    this.tokenStatus = v;
  }

  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String v) {
    this.orgId = v;
  }

  @DynamoDbSecondaryPartitionKey(indexNames = TablesAndIndexes.TEAM_TO_AUTHORIZATION_TOKEN_GSI)
  @DynamoDbAttribute(TableAttributes.TEAM_ID)
  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String v) {
    this.teamId = v;
  }

  @DynamoDbAttribute(TableAttributes.AUTHORIZATION_TOKEN_ISSUER)
  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String v) {
    this.issuer = v;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Instant getCreated() {
    return created;
  }

  public void setCreated(Instant v) {
    this.created = v;
  }

  @DynamoDbAttribute(TableAttributes.EXPIRY_TIME)
  public Long getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Long v) {
    this.expiryTime = v;
  }

  @DynamoDbAttribute(TableAttributes.AUTHORIZATION_TOKEN_ROLES)
  public List<String> getAuthorizationRoles() {
    return authorizationRoles;
  }

  public void setAuthorizationRoles(List<String> v) {
    this.authorizationRoles = v;
  }
}

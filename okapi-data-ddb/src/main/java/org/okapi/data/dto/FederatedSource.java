/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@AllArgsConstructor
@Builder
@DynamoDbBean
@NoArgsConstructor
public class FederatedSource {
  String orgId;
  String sourceName;
  String sourceType;
  String registrationToken;
  Instant created;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  public FederatedSource setOrgId(String orgId) {
    this.orgId = orgId;
    return this;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.SOURCE_ID)
  public String getSourceName() {
    return sourceName;
  }

  public FederatedSource setSourceName(String sourceName) {
    this.sourceName = sourceName;
    return this;
  }

  @DynamoDbAttribute(TableAttributes.SOURCE_TYPE)
  public String getSourceType() {
    return sourceType;
  }

  public FederatedSource setSourceType(String sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Instant getCreated() {
    return created;
  }

  public FederatedSource setCreated(Instant created) {
    this.created = created;
    return this;
  }

  @DynamoDbAttribute(TableAttributes.REGISTRATION_TOKEN)
  public String getRegistrationToken() {
    return registrationToken;
  }

  public FederatedSource setRegistrationToken(String registrationToken) {
    this.registrationToken = registrationToken;
    return this;
  }
}

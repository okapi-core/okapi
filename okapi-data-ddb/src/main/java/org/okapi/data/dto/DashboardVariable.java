/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Setter
public class DashboardVariable {
  public enum DASHBOARD_VAR_TYPE {
    METRIC,
    SVC,
    TAG
  }

  private String orgDashKey;
  private String varName;
  private String tag;

  private DASHBOARD_VAR_TYPE varType;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_DASH_KEY)
  public String getOrgDashKey() {
    return orgDashKey;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.DASH_VAR_NAME)
  public String getVarName() {
    return varName;
  }

  @DynamoDbAttribute(TableAttributes.DASH_VAR_TAG)
  public String getTag() {
    return tag;
  }

  @DynamoDbAttribute(TableAttributes.DASH_VAR_TYPE)
  public DASHBOARD_VAR_TYPE getVarType() {
    return varType;
  }

  public static String orgDashKey(String orgId, String dashboardId, String versionId) {
    return orgId + "#" + dashboardId + "#" + versionId;
  }
}

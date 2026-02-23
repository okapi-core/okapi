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
@Builder
@Setter
public class DashboardVersion {
  private String orgId;
  private String dashboardVersionId;
  private String dashboardId;
  private String versionId;
  private String status;
  private Long createdAt;
  private String createdBy;
  private String specHash;
  private String note;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.DASHBOARD_VERSION_ID)
  public String getDashboardVersionId() {
    return dashboardVersionId;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_ID)
  public String getDashboardId() {
    return dashboardId;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_VERSION)
  public String getVersionId() {
    return versionId;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_VERSION_STATUS)
  public String getStatus() {
    return status;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Long getCreatedAt() {
    return createdAt;
  }

  @DynamoDbAttribute(TableAttributes.CREATOR_ID)
  public String getCreatedBy() {
    return createdBy;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_VERSION_SPEC_HASH)
  public String getSpecHash() {
    return specHash;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_NOTE)
  public String getNote() {
    return note;
  }

  public static String dashboardVersionId(String dashboardId, String versionId) {
    return dashboardId + "#" + versionId;
  }
}

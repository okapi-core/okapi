package org.okapi.data.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardDdb {

  private String dashboardId;
  private String orgId;
  private String creator;
  private String lastEditor;
  private Instant created;
  private Instant updatedTime;
  private String dashboardNote;
  private String dashboardTitle;
  private DashboardDto.DASHBOARD_STATUS dashboardStatus;
  private Long version;
  private String bucket;
  private String prefix;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.DASHBOARD_ID)
  public String getDashboardId() {
    return dashboardId;
  }
  public void setDashboardId(String dashboardId) {
    this.dashboardId = dashboardId;
  }

  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }
  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_CREATOR)
  public String getCreator() {
    return creator;
  }
  public void setCreator(String creator) {
    this.creator = creator;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_LAST_EDITOR)
  public String getLastEditor() {
    return lastEditor;
  }
  public void setLastEditor(String lastEditor) {
    this.lastEditor = lastEditor;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Instant getCreated() {
    return created;
  }
  public void setCreated(Instant created) {
    this.created = created;
  }

  @DynamoDbAttribute(TableAttributes.UPDATED_TIME)
  public Instant getUpdatedTime() {
    return updatedTime;
  }
  public void setUpdatedTime(Instant updatedTime) {
    this.updatedTime = updatedTime;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_NOTE)
  public String getDashboardNote() {
    return dashboardNote;
  }
  public void setDashboardNote(String dashboardNote) {
    this.dashboardNote = dashboardNote;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_TITLE)
  public String getDashboardTitle() {
    return dashboardTitle;
  }
  public void setDashboardTitle(String dashboardTitle) {
    this.dashboardTitle = dashboardTitle;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_STATUS)
  public DashboardDto.DASHBOARD_STATUS getDashboardStatus() {
    return dashboardStatus;
  }
  public void setDashboardStatus(DashboardDto.DASHBOARD_STATUS dashboardStatus) {
    this.dashboardStatus = dashboardStatus;
  }

  @DynamoDbVersionAttribute
  @DynamoDbAttribute(TableAttributes.VERSION)
  public Long getVersion() {
    return version;
  }
  public void setVersion(Long version) {
    this.version = version;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_DEF_BUCKET)
  public String getBucket() {
    return bucket;
  }
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_DFE_PREFIX)
  public String getPrefix() {
    return prefix;
  }
  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }
}

package org.okapi.data.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.dashboardvars.DashVars;
import org.okapi.data.ddb.attributes.ResourceOrder;
import org.okapi.data.ddb.attributes.TagsList;
import org.okapi.data.ddb.attributes.serialization.ResourceOrderConverterGson;
import org.okapi.data.ddb.attributes.serialization.TagsListConverterGson;
import software.amazon.awssdk.enhanced.dynamodb.extensions.annotations.DynamoDbVersionAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Setter
public class DashboardDdb {

  private String orgId;
  private String dashboardId;

  // audit stuff
  private String creator;
  private String lastEditor;
  private Instant created;
  private Instant updatedTime;

  private String title;
  private String desc;
  private TagsList tags;
  private ResourceOrder rowOrder;
  private String activeVersion;

  private DashVars dashVars;
  // optimistic locking
  private Long version;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_ID)
  public String getOrgId() {
    return orgId;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.DASHBOARD_ID)
  public String getDashboardId() {
    return dashboardId;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_CREATOR)
  public String getCreator() {
    return creator;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_LAST_EDITOR)
  public String getLastEditor() {
    return lastEditor;
  }

  @DynamoDbAttribute(TableAttributes.CREATED_TIME)
  public Instant getCreated() {
    return created;
  }

  @DynamoDbAttribute(TableAttributes.UPDATED_TIME)
  public Instant getUpdatedTime() {
    return updatedTime;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_NOTE)
  public String getTitle() {
    return title;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_TITLE)
  public String getDesc() {
    return desc;
  }

  @DynamoDbVersionAttribute
  @DynamoDbAttribute(TableAttributes.VERSION)
  public Long getVersion() {
    return version;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_TAGS)
  @DynamoDbConvertedBy(TagsListConverterGson.class)
  public TagsList getTags() {
    return tags;
  }

  @DynamoDbAttribute(TableAttributes.ROW_ORDER)
  @DynamoDbConvertedBy(ResourceOrderConverterGson.class)
  public ResourceOrder getRowOrder() {
    return rowOrder;
  }

  @DynamoDbAttribute(TableAttributes.DASHBOARD_ACTIVE_VERSION)
  public String getActiveVersion() {
    return activeVersion;
  }
}

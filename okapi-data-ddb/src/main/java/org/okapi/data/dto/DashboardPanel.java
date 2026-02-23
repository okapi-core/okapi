package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.ddb.attributes.MultiQueryPanelConfig;
import org.okapi.data.ddb.attributes.serialization.MultiQueryPanelConfigConverterGson;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class DashboardPanel {
  String orgPanelHashKey;
  String panelId;
  String note;
  String title;

  MultiQueryPanelConfig queryConfig;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_PANEL_HASH_KEY)
  public String getOrgPanelHashKey() {
    return orgPanelHashKey;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.PANEL_ID)
  public String getPanelId() {
    return panelId;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_NOTE)
  public String getNote() {
    return note;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_TITLE)
  public String getTitle() {
    return title;
  }

  @DynamoDbAttribute(TableAttributes.PANEL_QUERY_CONFIG)
  @DynamoDbConvertedBy(MultiQueryPanelConfigConverterGson.class)
  public MultiQueryPanelConfig getQueryConfig() {
    return queryConfig;
  }
}

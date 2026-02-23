package org.okapi.data.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.okapi.data.ddb.attributes.ResourceOrder;
import org.okapi.data.ddb.attributes.serialization.ResourceOrderConverterGson;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

@DynamoDbBean
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class DashboardRow {
  String orgDashKey;
  String rowId;
  String note;
  String title;
  ResourceOrder panelOrder;

  @DynamoDbPartitionKey
  @DynamoDbAttribute(TableAttributes.ORG_DASH_KEY)
  public String getOrgDashKey() {
    return orgDashKey;
  }

  @DynamoDbSortKey
  @DynamoDbAttribute(TableAttributes.ROW_ID)
  public String getRowId() {
    return rowId;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_NOTE)
  public String getNote() {
    return note;
  }

  @DynamoDbAttribute(TableAttributes.ASSET_TITLE)
  public String getTitle() {
    return title;
  }

  @DynamoDbAttribute(TableAttributes.PANEL_ORDER)
  @DynamoDbConvertedBy(ResourceOrderConverterGson.class)
  public ResourceOrder getPanelOrder() {
    return panelOrder;
  }
}

package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.MultiQueryPanelConfig;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class MultiQueryPanelConfigConverterGson extends GenericGsonObjConverter<MultiQueryPanelConfig> {

  @Override
  public Class<MultiQueryPanelConfig> getClazz() {
    return MultiQueryPanelConfig.class;
  }

  @Override
  public EnhancedType<MultiQueryPanelConfig> type() {
    return EnhancedType.of(MultiQueryPanelConfig.class);
  }
}

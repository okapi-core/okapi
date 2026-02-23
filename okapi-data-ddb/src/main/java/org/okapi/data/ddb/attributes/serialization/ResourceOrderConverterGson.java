package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.ResourceOrder;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

public class ResourceOrderConverterGson extends GenericGsonObjConverter<ResourceOrder> {
  @Override
  public Class<ResourceOrder> getClazz() {
    return ResourceOrder.class;
  }

  @Override
  public EnhancedType<ResourceOrder> type() {
    return EnhancedType.of(ResourceOrder.class);
  }
}

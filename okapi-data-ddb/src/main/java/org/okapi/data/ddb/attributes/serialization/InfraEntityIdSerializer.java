package org.okapi.data.ddb.attributes.serialization;

import org.okapi.data.ddb.attributes.INFRA_ENTITY_TYPE;
import org.okapi.data.ddb.attributes.InfraEntityId;

public class InfraEntityIdSerializer implements CustomSerializer<InfraEntityId> {
  @Override
  public String serialize(InfraEntityId obj) {
    return obj.getTenantId() + ":" + obj.getEntityType() + ":" + obj.getId();
  }

  @Override
  public InfraEntityId deserialize(String str) {
    var parts = str.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid InfraEntityId format: " + str);
    }
    return new InfraEntityId(parts[0], INFRA_ENTITY_TYPE.valueOf(parts[1]), parts[2]);
  }

  public static final InfraEntityIdSerializer INSTANCE = new InfraEntityIdSerializer();

  public static InfraEntityIdSerializer getSingleton() {
    return INSTANCE;
  }
}

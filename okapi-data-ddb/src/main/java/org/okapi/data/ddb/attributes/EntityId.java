package org.okapi.data.ddb.attributes;

import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EntityId {
  ENTITY_TYPE type;
  String id;

  public String id() {
    return id;
  }

  public ENTITY_TYPE type() {
    return type;
  }

  public String toString() {
    return type.name() + ":" + id;
  }

  public static EntityId of(ENTITY_TYPE type, String id) {
    return new EntityId(type, id);
  }

  public static Optional<EntityId> parse(String id) {
    String[] parts = id.split(":");
    return parts.length != 2
        ? Optional.empty()
        : Optional.of(new EntityId(ENTITY_TYPE.valueOf(parts[0]), parts[1]));
  }
}

package org.okapi.data.dto;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Builder
public class RelationGraphNode {

  public record EntityId(ENTITY_TYPE type, String id) {}

  public enum ENTITY_TYPE {
    USER,
    ORG,
    TEAM,
    DASHBOARD
  }

  //  Add more relations as required
  public enum RELATION_TYPE {
    // org related
    ORG_MEMBER,
    ORG_ADMIN,
    // team related
    TEAM_MEMBER,
    TEAM_ADMIN,
    // dashboard related
    DASHBOARD_EDIT,
    DASHBOARD_READ
  }

  @Getter String entityId;
  @Getter String relatedEntity;
  @Getter List<RELATION_TYPE> relationType;
  @Getter Integer version;

  public static String makeEntityId(ENTITY_TYPE type, String id) {
    return type.name() + ":" + id;
  }

  public static Optional<EntityId> parse(String id) {
    var parts = id.split(":");
    if (parts.length != 2) return Optional.empty();
    else return Optional.of(new EntityId(ENTITY_TYPE.valueOf(parts[0]), parts[1]));
  }
}

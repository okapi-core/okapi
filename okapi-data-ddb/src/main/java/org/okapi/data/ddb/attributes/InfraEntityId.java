package org.okapi.data.ddb.attributes;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class InfraEntityId {
  String tenantId;
  INFRA_ENTITY_TYPE entityType;
  String id;
}

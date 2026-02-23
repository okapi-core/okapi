package org.okapi.data.ddb.attributes;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class InfraNodeOutgoingEdge {
  InfraEntityId targetNodeId;
  String edgeAttributes;
  DEP_TYPE depType;
}

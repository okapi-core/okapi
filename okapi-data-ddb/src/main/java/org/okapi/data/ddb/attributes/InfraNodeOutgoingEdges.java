package org.okapi.data.ddb.attributes;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InfraNodeOutgoingEdges {
  List<InfraNodeOutgoingEdge> edges;
}

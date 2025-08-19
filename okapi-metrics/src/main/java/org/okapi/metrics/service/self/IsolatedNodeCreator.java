package org.okapi.metrics.service.self;

import lombok.AllArgsConstructor;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;

@AllArgsConstructor
public class IsolatedNodeCreator implements NodeCreator {
  String nodeId;

  @Override
  public Node whoAmI() {
    return new Node(nodeId, "localhost", NodeState.NODE_CREATED);
  }
}

package org.okapi.metrics.service.self;

import org.apache.commons.lang3.RandomStringUtils;
import org.okapi.ip.IpSupplier;
import org.okapi.metrics.common.pojo.Node;
import org.okapi.metrics.common.pojo.NodeState;

public class UniqueNodeCreator implements NodeCreator {

  Node node;

  public UniqueNodeCreator(IpSupplier ipSupplier) {
    var id = RandomStringUtils.secure().next(10, true, true);
    this.node = new Node(id, ipSupplier.getIp(), NodeState.METRICS_CONSUMPTION_START);
  }

  @Override
  public Node whoAmI() {
    return node;
  }
}

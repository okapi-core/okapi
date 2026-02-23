package org.okapi.identity;

import java.util.UUID;

public interface WhoAmI {
  String getNodeIp();

  int getNodePort();

  default String getNodeId() {
    return UUID.randomUUID().toString();
  }
}

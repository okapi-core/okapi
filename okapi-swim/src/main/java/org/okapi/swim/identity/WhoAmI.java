package org.okapi.swim.identity;

public interface WhoAmI {
  String getNodeId();
  String getNodeIp();
  int getNodePort();
}


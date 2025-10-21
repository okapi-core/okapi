package org.okapi.swim;

import org.okapi.swim.identity.WhoAmI;

public class TestWhoAmiFactory {

  public static WhoAmI makeWhoAmI(String nodeId, String ip, int port) {
    return new WhoAmI() {
      @Override
      public String getNodeId() {
        return nodeId;
      }

      @Override
      public String getNodeIp() {
        return ip;
      }

      @Override
      public int getNodePort() {
        return port;
      }
    };
  }
}

package org.okapi.swim.k8s;

import java.net.InetAddress;
import java.util.UUID;
import org.okapi.swim.identity.WhoAmI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("k8s")
public class K8sWhoAmI implements WhoAmI {

  @Value("${server.port}")
  private int serverPort;

  private static String env(String key) {
    String v = System.getenv(key);
    return v != null ? v : "";
  }

  @Override
  public String getNodeId() {
    String host = env("HOSTNAME");
    if (!host.isEmpty()) return host;
    return UUID.randomUUID().toString();
  }

  @Override
  public String getNodeIp() {
    String ip = env("POD_IP");
    if (!ip.isEmpty()) return ip;
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      return "127.0.0.1";
    }
  }

  @Override
  public int getNodePort() {
    return serverPort;
  }
}

package org.okapi.nodes;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FixedIpSupplier implements IpSupplier {
  String host;

  @Override
  public String getIp() {
    return host;
  }
}

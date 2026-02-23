/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
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

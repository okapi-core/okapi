/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.identity;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Member {
  String nodeId;
  String ip;
  int port;

  public Member(String nodeId, String ip, int port) {
    this.nodeId = Preconditions.checkNotNull(nodeId);
    this.ip = Preconditions.checkNotNull(ip);
    this.port = Preconditions.checkNotNull(port);
  }
}

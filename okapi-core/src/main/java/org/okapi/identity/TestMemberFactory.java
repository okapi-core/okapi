/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class TestMemberFactory {

  public static List<Member> makeMemberList(int clusterSize) {
    return Collections.unmodifiableList(
        new ArrayList<>() {
          {
            for (int i = 0; i < clusterSize; i++) {
              add(getRandomMember());
            }
          }
        });
  }

  public static Member getRandomMember() {
    return new Member(UUID.randomUUID().toString(), "ip_" + UUID.randomUUID().toString(), 8080);
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.identity;

import java.util.HashMap;
import java.util.Map;

public class InMemoryMemberLists implements MemberList {

  WhoAmI whoAmI;
  Map<String, Member> members = new HashMap<>();

  public InMemoryMemberLists(WhoAmI whoAmI) {
    this.whoAmI = whoAmI;
  }

  public void addMember(Member member) {
    members.put(member.getNodeId(), member);
  }

  @Override
  public Member getMember(String nodeId) {
    return this.members.get(nodeId);
  }
}

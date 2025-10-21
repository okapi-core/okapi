package org.okapi.swim.bootstrap;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeedMembersInitializer {
  private final MemberList memberList;

  @Autowired(required = false)
  private SeedMembersProvider seedMembersProvider;

  @PostConstruct
  public void init() {
    if (seedMembersProvider == null) return;
    List<Member> seeds = seedMembersProvider.getSeedMembers();
    if (seeds == null || seeds.isEmpty()) return;
    for (Member m : seeds) {
      if (m != null && m.getNodeId() != null) {
        memberList.addMember(m);
      }
    }
  }
}


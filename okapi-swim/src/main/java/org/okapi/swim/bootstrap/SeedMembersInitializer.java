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

  @Autowired private SeedMembersProvider seedMembersProvider;

  @PostConstruct
  public void init() throws InterruptedException {
    List<Member> seeds = seedMembersProvider.getSeedMembers();
    if (seeds == null || seeds.isEmpty()) return;
    for (var member : seeds) {
      if (member != null && member.getNodeId() != null) {
        memberList.addMember(member);
      }
    }
  }
}

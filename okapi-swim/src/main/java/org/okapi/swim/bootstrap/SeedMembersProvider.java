package org.okapi.swim.bootstrap;

import java.util.List;
import org.okapi.swim.ping.Member;

public interface SeedMembersProvider {
  List<Member> getSeedMembers();
}


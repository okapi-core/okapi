package org.okapi.logs.select;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.okapi.logs.StaticConfiguration;
import org.okapi.swim.hash.RendezvousHasher;
import org.okapi.swim.ping.Member;
import org.okapi.swim.ping.MemberList;
import org.springframework.stereotype.Component;

@Component
public class BlockMemberSelector {
  public Member select(String tenantId, String logStream, long hourStartMillis, int blockIdx, MemberList memberList) {
    // Group members by block
    var grouped = new HashMap<Integer, List<Member>>();
    for (Member m : memberList.getAllMembers()) {
      int b = StaticConfiguration.rkHash(m.getNodeId());
      grouped.computeIfAbsent(b, k -> new ArrayList<>()).add(m);
    }

    int targetBlock = blockIdx;
    // find first non-empty block if target is empty
    while (grouped.getOrDefault(targetBlock, Collections.emptyList()).isEmpty()) {
      targetBlock = (targetBlock + 1) % StaticConfiguration.N_BLOCKS;
    }
    List<Member> candidates = grouped.get(targetBlock);
    byte[] key = RendezvousHasher.key(tenantId, logStream, hourStartMillis, blockIdx);
    return RendezvousHasher.select(candidates, key);
  }
}


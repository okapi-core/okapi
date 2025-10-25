package org.okapi.swim.ping;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.swim.disseminate.Disseminator;
import org.okapi.swim.identity.WhoAmI;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PingBatchJob {
  private final MemberList memberList;
  private final PingService pingService;
  private final Disseminator disseminator;
  private final WhoAmI whoAmI;

  // Actively probe peers. Default every 2s; override with okapi.swim.pingSchedulerDelayMillis
  @Scheduled(fixedDelayString = "${okapi.swim.pingSchedulerDelayMillis:2000}")
  public void pingMember() throws ExecutionException, InterruptedException, TimeoutException {
    List<Member> all = memberList.getAllMembers();
    if (all == null || all.isEmpty()) return;

    // avoid self where possible
    Member target = null;
    for (int i = 0; i < Math.min(all.size(), 3); i++) {
      Member candidate = memberList.sample();
      if (!candidate.getNodeId().equals(whoAmI.getNodeId())) {
        target = candidate;
        break;
      }
    }
    if (target == null) return; // only self present

    var result = pingService.ping(target.getNodeId());
    if (result.getException() != null) {
      var indirect = pingService.pingKIndirect(target.getNodeId());
      if (indirect.isEmpty()) {
        // direct + indirect failed: disseminate unhealthy to peers
        disseminator.disseminateUnHealthy(target.getNodeId());
      }
    }
  }
}

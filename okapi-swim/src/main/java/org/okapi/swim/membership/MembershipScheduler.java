package org.okapi.swim.membership;

import lombok.RequiredArgsConstructor;
import org.okapi.swim.config.SwimConfig;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MembershipScheduler {
  private final MembershipService membershipService;
  private final SwimConfig swimConfig;

  @Scheduled(fixedDelayString = "${okapi.swim.suspectSchedulerDelayMillis:5000}")
  public void expireSuspicions() {
    long timeout = swimConfig.getSuspectTimeoutMillis() > 0 ? swimConfig.getSuspectTimeoutMillis() : 30000;
    membershipService.tickExpireSuspicions(timeout);
  }
}


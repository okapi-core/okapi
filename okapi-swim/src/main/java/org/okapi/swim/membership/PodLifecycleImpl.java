package org.okapi.swim.membership;

import lombok.RequiredArgsConstructor;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.time.SwimClock;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PodLifecycleImpl implements PodLifecycle {
  private final MembershipEventPublisher publisher;
  private final WhoAmI whoAmI;
  private final SwimClock clock;

  @Override
  public void emitPodDelete() {
    publisher.emitPodDelete(whoAmI, clock.nowMillis());
  }
}


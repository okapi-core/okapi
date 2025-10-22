package org.okapi.swim.membership;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.okapi.swim.identity.WhoAmI;
import org.okapi.swim.time.SwimClock;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartupPodAddEmitter {
  private final MembershipEventPublisher publisher;
  private final WhoAmI whoAmI;
  private final SwimClock clock;

  @PostConstruct
  public void onStart() {
    publisher.emitPodAdd(whoAmI, clock.nowMillis());
  }
}


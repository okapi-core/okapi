package org.okapi.swim.membership;

import org.okapi.swim.identity.WhoAmI;

public interface MembershipEventPublisher {
  void emitPodAdd(WhoAmI whoAmI, long timestampMillis);

  void emitPodDelete(WhoAmI whoAmI, long timestampMillis);
}


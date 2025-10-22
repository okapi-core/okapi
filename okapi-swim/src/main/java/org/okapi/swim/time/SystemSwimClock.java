package org.okapi.swim.time;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class SystemSwimClock implements SwimClock {
  @Override
  public long nowMillis() {
    return System.currentTimeMillis();
  }
}


package org.okapi.fake;

import com.google.common.base.Preconditions;
import org.okapi.clock.Clock;
import lombok.Getter;

public class FakeClock implements Clock {

  long inc;

  public FakeClock(long inc) {
    Preconditions.checkArgument(inc > 0, "Inc should be positive");
    this.inc = inc;
  }

  @Getter long time = -inc;

  @Getter long count = 0;

  public FakeClock setTime(long time) {
    this.time = time;
    return this;
  }

  @Override
  public long currentTimeMillis() {
    count++;
    time += inc;
    return time;
  }
}

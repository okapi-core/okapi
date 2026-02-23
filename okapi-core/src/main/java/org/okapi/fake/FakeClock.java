package org.okapi.fake;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.okapi.clock.Clock;

public class FakeClock implements Clock {

  long inc;
  @Getter long time = -inc;
  @Getter long count = 0;

  public FakeClock(long inc) {
    Preconditions.checkArgument(inc > 0, "Inc should be positive");
    this.inc = inc;
  }

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

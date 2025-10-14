package org.okapi.clock;

public interface Clock {
  long currentTimeMillis();

  Clock setTime(long time);

  long getTime();
}

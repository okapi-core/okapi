package org.okapi.clock;

public interface Clock {
  long currentTimeMillis();

  long getTime();

  Clock setTime(long time);
}

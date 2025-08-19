package org.okapi.wal.it.env;

import java.time.*;

public final class TestClock extends Clock {
  private Instant now;
  private final ZoneId zone;

  public TestClock(Instant initial, ZoneId zone) {
    this.now = initial;
    this.zone = zone;
  }

  public static TestClock startingAt(Instant instant) {
    return new TestClock(instant, ZoneOffset.UTC);
  }

  public void advance(Duration d) {
    now = now.plus(d);
  }

  @Override
  public ZoneId getZone() {
    return zone;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return new TestClock(now, zone);
  }

  @Override
  public Instant instant() {
    return now;
  }
}

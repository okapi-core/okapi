package org.okapi.wal.it.faults;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class CrashInjector {
  private final Map<CrashPoint, AtomicInteger> triggers = new EnumMap<>(CrashPoint.class);

  public void arm(CrashPoint p) {
    triggers.put(p, new AtomicInteger(1));
  }

  public void arm(CrashPoint p, int onNth) {
    triggers.put(p, new AtomicInteger(onNth));
  }

  public void hit(CrashPoint p) {
    AtomicInteger c = triggers.get(p);
    if (c == null) return;
    if (c.decrementAndGet() == 0) {
      throw new RuntimeException("Injected crash at " + p);
    }
  }
}

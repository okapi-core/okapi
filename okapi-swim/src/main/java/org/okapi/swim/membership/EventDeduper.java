package org.okapi.swim.membership;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class EventDeduper {
  public record Key(String type, String nodeId, long incarnation) {}

  private final Map<Key, Instant> seen = new ConcurrentHashMap<>();
  private final long ttlMillis;
  private final int maxEntries;

  public EventDeduper(long ttlMillis, int maxEntries) {
    this.ttlMillis = ttlMillis;
    this.maxEntries = maxEntries;
  }

  public boolean seenOnce(Key key) {
    prune();
    return seen.putIfAbsent(key, Instant.now()) == null;
  }

  private void prune() {
    if (seen.size() > maxEntries) {
      // simple size guard prune: remove oldest entries
      var now = Instant.now();
      seen.entrySet().removeIf(e -> now.toEpochMilli() - e.getValue().toEpochMilli() > ttlMillis);
      if (seen.size() > maxEntries) {
        // if still over capacity, drop arbitrary overflow to prevent unbounded growth
        int toRemove = seen.size() - maxEntries;
        var it = seen.keySet().iterator();
        while (toRemove-- > 0 && it.hasNext()) {
          it.remove();
        }
      }
    } else {
      var now = Instant.now();
      seen.entrySet().removeIf(e -> now.toEpochMilli() - e.getValue().toEpochMilli() > ttlMillis);
    }
  }
}


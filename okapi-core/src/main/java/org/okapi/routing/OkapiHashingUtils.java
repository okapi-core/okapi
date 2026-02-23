package org.okapi.routing;

import java.util.Map;
import java.util.Objects;

public class OkapiHashingUtils {
  public static int computeHash(String str, Map<String, String> map) {
    int result = str.hashCode();

    // Compute a combined hash of the map entries, order-independent
    int mapHash =
        map.entrySet().stream()
            .map(entry -> Objects.hash(entry.getKey(), entry.getValue()))
            .sorted() // ensure consistent ordering regardless of map insertion order
            .reduce(0, (a, b) -> 31 * a + b);

    return 31 * result + mapHash;
  }
}

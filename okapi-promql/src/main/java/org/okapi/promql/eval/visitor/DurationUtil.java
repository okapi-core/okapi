/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.promql.eval.visitor;

// parse/DurationUtil.java
public final class DurationUtil {
  private DurationUtil() {}

  /**
   * Prom-like duration: 5s, 1m, 2h, 7d, 2w, 1y Also accepts plain numbers (e.g., "15" or "0.5")
   * meaning seconds (Grafana step).
   */
  public static long parseToMillis(String dur) {
    if (dur == null) throw new IllegalArgumentException("Bad duration: null");
    String s = dur.trim();
    if (s.isEmpty()) throw new IllegalArgumentException("Bad duration: \"\"");

    // If it's purely a number (integer or decimal), interpret as seconds.
    boolean numericOrDecimal = true;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!(Character.isDigit(c) || c == '.')) {
        numericOrDecimal = false;
        break;
      }
    }
    if (numericOrDecimal) {
      // Supports "15", "0.5", "1.25" -> seconds
      java.math.BigDecimal seconds = new java.math.BigDecimal(s);
      return seconds
          .multiply(java.math.BigDecimal.valueOf(1000))
          .setScale(0, java.math.RoundingMode.HALF_UP)
          .longValueExact();
    }

    long n = 0;
    int i = 0, len = s.length();
    while (i < len && Character.isDigit(s.charAt(i))) {
      n = n * 10 + (s.charAt(i++) - '0');
    }
    if (i >= len) {
      // String had digits only (caught above), or invalid form like "123 " (trimmed already)
      throw new IllegalArgumentException("Bad duration: " + dur);
    }

    // Handle units
    char u = Character.toLowerCase(s.charAt(i));
    switch (u) {
      case 's':
        return n * 1000L;
      case 'm':
        return n * 60_000L;
      case 'h':
        return n * 3_600_000L;
      case 'd':
        return n * 86_400_000L;
      case 'w':
        return n * 7L * 86_400_000L;
      case 'y':
        return n * 365L * 86_400_000L;
      default:
        throw new IllegalArgumentException("Unsupported unit: " + u + " in " + dur);
    }
  }
}

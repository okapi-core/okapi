package org.okapi.logs.io;

import java.util.ArrayList;
import java.util.List;

final class TrigramUtil {
  private TrigramUtil() {}

  static List<Integer> extractAsciiTrigramIndices(String s) {
    List<Integer> out = new ArrayList<>();
    if (s == null) return out;
    int n = s.length();
    char[] buf = s.toCharArray();
    for (int i = 0; i + 2 < n; i++) {
      char c0 = buf[i];
      char c1 = buf[i + 1];
      char c2 = buf[i + 2];
      if (c0 < 128 && c1 < 128 && c2 < 128) {
        int idx = (c0 & 0x7F) | ((c1 & 0x7F) << 7) | ((c2 & 0x7F) << 14);
        out.add(idx);
      }
    }
    return out;
  }
}


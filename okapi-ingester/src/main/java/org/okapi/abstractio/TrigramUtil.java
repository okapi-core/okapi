/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.abstractio;

import java.util.ArrayList;
import java.util.List;

public final class TrigramUtil {
  private TrigramUtil() {}

  public static List<Integer> extractAsciiTrigramIndices(String s) {
    List<Integer> out = new ArrayList<>();
    if (s == null) return out;
    int n = s.length();
    char[] buf = s.toCharArray();
    for (int i = 0; i + 2 < n; i++) {
      char c0 = buf[i];
      char c1 = buf[i + 1];
      char c2 = buf[i + 2];
      if (c0 < 128 && c1 < 128 && c2 < 128) {
        var idx = getTrigramIndex(c0, c1, c2);
        out.add(idx);
      }
    }
    return out;
  }

  public static Integer getTrigramIndex(char c0, char c1, char c2) {
    return (c0 & 0x7F) | ((c1 & 0x7F) << 7) | ((c2 & 0x7F) << 14);
  }

  public static List<List<Integer>> getOrTrigramsBasedOnRe2(String regex) {
    var split = regex.split("\\|");
    List<List<Integer>> out = new ArrayList<>();
    for (var part : split) {
      var trigrams = extractTrigramForRe2SingleBlock(part);
      out.add(trigrams);
    }
    return out;
  }

  public static List<Integer> extractTrigramForRe2SingleBlock(String regex) {
    var len = regex.length();
    var indices = new ArrayList<Integer>();
    for (int i = 0; i < len - 2; i++) {
      char c0 = regex.charAt(i);
      char c1 = regex.charAt(i + 1);
      char c2 = regex.charAt(i + 2);
      if (isLiteralChar(c0) && isLiteralChar(c1) && isLiteralChar(c2)) {
        var idx = getTrigramIndex(c0, c1, c2);
        indices.add(idx);
      }
    }
    return indices;
  }

  public static boolean isLiteralChar(char c) {
    return (c >= 'a' && c <= 'z')
        || (c >= 'A' && c <= 'Z')
        || (c >= '0' && c <= '9')
        || (c == ' ')
        || (c == ',')
        || (c == '_')
        || (c == '-');
  }
}

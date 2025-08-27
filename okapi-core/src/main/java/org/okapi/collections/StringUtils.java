package org.okapi.collections;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

  public static List<String> splitLeft(String ref, char sep, int nSplits) {
    var splits = new ArrayList<String>();
    var sb = new StringBuilder();
    var found = 0;
    for (char c : ref.toCharArray()) {
      if (c == sep && !sb.isEmpty() && found < nSplits) {
        splits.add(sb.toString());
        sb.setLength(0);
        found++;
      } else {
        sb.append(c);
      }
    }

    if (!sb.isEmpty()) {
      splits.add(sb.toString());
    }
    return splits;
  }
}

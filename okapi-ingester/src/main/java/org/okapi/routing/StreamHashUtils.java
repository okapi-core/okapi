package org.okapi.routing;

public class StreamHashUtils {

  public static final int hashCode(String s, int block) {
    var h = 0;
    for (int i = 0; i < s.length(); i++) {
      h = 31 * h + s.charAt(i);
    }
    return Math.abs(h % block);
  }
}

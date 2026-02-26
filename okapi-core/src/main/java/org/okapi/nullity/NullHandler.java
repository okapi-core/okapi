package org.okapi.nullity;

import java.util.Optional;
import java.util.function.Function;
import org.apache.logging.log4j.util.Strings;
import org.checkerframework.checker.units.qual.A;

public class NullHandler {
  public static <T> T ifNullThen(T obj, T default_) {
    if (obj == null) {
      return default_;
    } else return obj;
  }

  public static String ifNullThenEmpty(String s) {
    return (s == null) ? Strings.EMPTY : s;
  }

  public static <A, B> Optional<B> safelyGet(A first, Function<A, B> second) {
    if (first == null) {
      return Optional.empty();
    }
    var next = second.apply(first);
    return Optional.ofNullable(next);
  }
}

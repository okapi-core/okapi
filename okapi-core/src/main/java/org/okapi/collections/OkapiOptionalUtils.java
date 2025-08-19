package org.okapi.collections;

import java.util.Collection;
import java.util.Optional;

public class OkapiOptionalUtils {
  public static <T> Optional<T> findPresent(Collection<Optional<T>> optionals) {
    for (Optional<T> optional : optionals) {
      if (optional.isPresent()) {
        return optional;
      }
    }
    return Optional.empty();
  }
}

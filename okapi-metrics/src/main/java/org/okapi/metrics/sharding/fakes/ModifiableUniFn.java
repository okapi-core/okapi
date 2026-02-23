package org.okapi.metrics.sharding.fakes;

import java.util.HashMap;
import java.util.Map;

public class ModifiableUniFn<T, U> {

  Map<T, U> values;

  public ModifiableUniFn() {
    this.values = new HashMap<>();
  }

  public void set(T t, U u) {
    values.put(t, u);
  }

  public U apply(T t) {
    return values.get(t);
  }
}

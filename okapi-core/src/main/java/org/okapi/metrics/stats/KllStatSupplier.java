package org.okapi.metrics.stats;

import java.util.function.Supplier;
import org.apache.datasketches.kll.KllFloatsSketch;

public class KllStatSupplier implements Supplier<UpdatableStatistics> {
  @Override
  public UpdatableStatistics get() {
    return new RolledUpStatistics(kllSketch());
  }

  public static KllFloatsSketch kllSketch() {
    return KllFloatsSketch.newHeapInstance(200);
  }
}

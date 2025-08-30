package org.okapi.metrics.stats;

import org.apache.datasketches.kll.KllFloatsSketch;

import java.util.function.Supplier;

public class KllStatSupplier implements Supplier<UpdatableStatistics> {
    @Override
    public UpdatableStatistics get() {
        return new RolledUpStatistics(kllSketch());
    }

    public static KllFloatsSketch kllSketch(){
        return KllFloatsSketch.newHeapInstance(200);
    }
}

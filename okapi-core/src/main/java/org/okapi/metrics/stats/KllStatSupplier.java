package org.okapi.metrics.stats;

import org.apache.datasketches.kll.KllFloatsSketch;

import java.util.function.Supplier;

public class KllStatSupplier implements Supplier<Statistics> {
    @Override
    public Statistics get() {
        return new RolledUpStatistics(KllFloatsSketch.newHeapInstance(200));
    }
}

package org.okapi.wal;

import java.util.function.LongSupplier;

public class OffsetLsnSupplier implements LongSupplier {
    long current = Long.MIN_VALUE + 1;
    @Override
    public long getAsLong() {
        return current++;
    }
}

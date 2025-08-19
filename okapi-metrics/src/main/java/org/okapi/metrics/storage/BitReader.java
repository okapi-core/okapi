package org.okapi.metrics.storage;

public interface BitReader {
    boolean nextBit();
    boolean hasNext();
}

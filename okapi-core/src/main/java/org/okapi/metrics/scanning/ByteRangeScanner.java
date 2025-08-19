package org.okapi.metrics.scanning;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface ByteRangeScanner {
    // read nb bytes from offset
    long totalBytes() throws IOException;
    byte[] getRange(long off, int nb) throws IOException, ExecutionException;
}

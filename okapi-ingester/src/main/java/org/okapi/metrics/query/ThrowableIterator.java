package org.okapi.metrics.query;

import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;

import java.io.IOException;
import java.util.Optional;

public interface ThrowableIterator<T, E extends Exception> {
  boolean hasMore();

  Optional<T> next() throws E, RangeIterationException, StreamReadingException, IOException, NotEnoughBytesException;
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.query;

import java.io.IOException;
import java.util.Optional;
import org.okapi.byterange.RangeIterationException;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;

public interface ThrowableIterator<T, E extends Exception> {
  boolean hasMore();

  Optional<T> next()
      throws E,
          RangeIterationException,
          StreamReadingException,
          IOException,
          NotEnoughBytesException;
}

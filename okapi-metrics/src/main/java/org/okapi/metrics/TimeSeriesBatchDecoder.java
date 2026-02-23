/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics;

public interface TimeSeriesBatchDecoder {
  boolean hasMore();

  // decodes values in the buffers represented by the values and times array. Returns the total
  // number of values decoded.
  int next(long[] times, float[] values);
}

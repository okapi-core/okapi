/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.stats;

import org.apache.datasketches.kll.KllFloatsSketch;

public interface QuantileRestorer {
  KllFloatsSketch restoreQuantiles(byte[] bytes) throws Exception;
}

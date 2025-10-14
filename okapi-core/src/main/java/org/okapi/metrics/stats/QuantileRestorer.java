package org.okapi.metrics.stats;

import org.apache.datasketches.kll.KllFloatsSketch;

public interface QuantileRestorer {
  KllFloatsSketch restoreQuantiles(byte[] bytes) throws Exception;
}

package org.okapi.metrics.stats;

import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.memory.Memory;

public class KllSketchRestorer implements QuantileRestorer {

  @Override
  public KllFloatsSketch restoreQuantiles(byte[] bytes) throws Exception {
    var mem = Memory.wrap(bytes);
    return KllFloatsSketch.heapify(mem);
  }

  public static KllFloatsSketch restoreFromBytes(byte[] bytes){
    var mem = Memory.wrap(bytes);
    return KllFloatsSketch.heapify(mem);
  }
}

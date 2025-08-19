package org.okapi.metrics.stats;


import org.apache.datasketches.kll.KllFloatsSketch;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.quantilescommon.QuantilesFloatsAPI;

public class KllSketchRestorer implements QuantileRestorer{

    @Override
    public QuantilesFloatsAPI restoreQuantiles(byte[] bytes) throws Exception {
        var mem = Memory.wrap(bytes);
        return KllFloatsSketch.heapify(mem);
    }
}

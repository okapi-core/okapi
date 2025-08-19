package org.okapi.metrics.stats;

import org.apache.datasketches.quantilescommon.QuantilesFloatsAPI;

public interface QuantileRestorer {
    QuantilesFloatsAPI restoreQuantiles(byte[] bytes) throws Exception;
}

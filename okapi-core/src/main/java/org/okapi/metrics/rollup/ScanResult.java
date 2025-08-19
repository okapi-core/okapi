package org.okapi.metrics.rollup;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
public class ScanResult {

    @Getter
    public final String seriesName;

    @Getter
    public final List<Long> timestamps;

    @Getter
    public final List<Float> values;
}

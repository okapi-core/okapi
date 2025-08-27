package org.okapi.metrics.rollup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
public class ScanResult {

    @Getter
    public final String seriesName;

    @Getter
    public final List<Long> timestamps;

    @Getter
    public final List<Float> values;

}

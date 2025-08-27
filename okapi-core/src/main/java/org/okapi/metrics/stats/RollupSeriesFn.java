package org.okapi.metrics.stats;

import org.okapi.metrics.rollup.RollupSeries;

import java.util.function.Function;

public class RollupSeriesFn implements Function<Integer, RollupSeries<Statistics>> {
    @Override
    public RollupSeries<Statistics> apply(Integer integer) {
        var statsSupplier = new KllStatSupplier();
        var series = new RollupSeries<>(statsSupplier, integer);
        return series;
    }
}

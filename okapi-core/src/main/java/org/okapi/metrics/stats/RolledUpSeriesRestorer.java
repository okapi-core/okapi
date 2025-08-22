package org.okapi.metrics.stats;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.rollup.RollupSeries;

public class RolledUpSeriesRestorer implements RollupSeriesRestorer<Statistics>{

    private final StatisticsRestorer<Statistics> restorer;
    private final Supplier<Statistics> newStatsSupplier;

    public RolledUpSeriesRestorer(StatisticsRestorer<Statistics> restorer,
                                  Supplier<Statistics> newStatsSupplier) {
        this.restorer = restorer;
        this.newStatsSupplier = newStatsSupplier;
    }

    @Override
    public RollupSeries<Statistics> restore(InputStream is) throws StreamReadingException, IOException {
        var series = new RollupSeries<Statistics>(restorer, newStatsSupplier);
        series.loadCheckpoint(is);
        return series;
    }

    public RollupSeries<Statistics> restore(Path checkpointPath)
            throws IOException, StreamReadingException {
        try (var fis = new FileInputStream(checkpointPath.toFile())) {
            return restore(fis);
        }
    }

}

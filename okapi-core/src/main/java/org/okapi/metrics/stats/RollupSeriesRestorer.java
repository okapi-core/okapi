package org.okapi.metrics.stats;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.rollup.RollupSeries;

public interface RollupSeriesRestorer<T extends  Statistics> {
    RollupSeries<T> restore(int shard, InputStream is) throws StreamReadingException, IOException;
    RollupSeries<T> restore(int shard, Path checkpoint) throws StreamReadingException, IOException;
}

package org.okapi.metrics.stats;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Function;
import org.okapi.metrics.io.StreamReadingException;
import org.okapi.metrics.rollup.RollupSeries;

public class RolledUpSeriesRestorer implements RollupSeriesRestorer<Statistics> {

  private final Function<Integer, RollupSeries<Statistics>> seriesSupplier;

  public RolledUpSeriesRestorer(Function<Integer, RollupSeries<Statistics>> seriesSupplier) {
    this.seriesSupplier = seriesSupplier;
  }

  @Override
  public RollupSeries<Statistics> restore(int shard, InputStream is)
      throws StreamReadingException, IOException {
    var series = seriesSupplier.apply(shard);
    series.loadCheckpoint(is);
    return series;
  }

  public RollupSeries<Statistics> restore(int shard, Path checkpointPath)
      throws IOException, StreamReadingException {
    try (var fis = new FileInputStream(checkpointPath.toFile())) {
      return restore(shard, fis);
    }
  }
}

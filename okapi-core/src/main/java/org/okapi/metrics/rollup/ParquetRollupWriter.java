package org.okapi.metrics.rollup;

import org.okapi.metrics.stats.Statistics;

import java.io.IOException;
import java.nio.file.Path;

public interface ParquetRollupWriter<T extends Statistics> {

  void open(String tenant, Path path) throws IOException;

  void consume(RollupSeries<T> series, long hr) throws IOException;

  void close() throws IOException;
}

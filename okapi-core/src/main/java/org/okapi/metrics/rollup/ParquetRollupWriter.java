package org.okapi.metrics.rollup;

import java.io.IOException;
import java.nio.file.Path;

public interface ParquetRollupWriter {

  void open(String tenant, Path path) throws IOException;

  void consume(RollupSeries series, long hr) throws IOException;

  void close() throws IOException;
}

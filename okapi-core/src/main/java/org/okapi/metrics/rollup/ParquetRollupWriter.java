package org.okapi.metrics.rollup;

import java.io.IOException;
import org.okapi.metrics.stats.Statistics;

public interface ParquetRollupWriter<T extends Statistics> {

  void writeDump(String tenantId, long hr) throws IOException;
}

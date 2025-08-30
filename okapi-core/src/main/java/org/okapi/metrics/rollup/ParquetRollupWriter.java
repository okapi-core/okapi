package org.okapi.metrics.rollup;

import java.io.IOException;
import org.okapi.metrics.stats.UpdatableStatistics;

public interface ParquetRollupWriter<T extends UpdatableStatistics> {

  void writeDump(String tenantId, long hr) throws IOException;
}

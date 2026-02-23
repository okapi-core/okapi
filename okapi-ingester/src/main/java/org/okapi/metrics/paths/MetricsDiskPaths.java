package org.okapi.metrics.paths;

import java.nio.file.Path;
import org.okapi.abstractio.ExpiryDurationPartitionedPaths;

public class MetricsDiskPaths extends ExpiryDurationPartitionedPaths<String> {
  public MetricsDiskPaths(Path dataDir, long idxExpiryDuration, String baseFileName) {
    super(dataDir, idxExpiryDuration, baseFileName);
  }
}

package org.okapi.traces.paths;

import java.nio.file.Path;
import org.okapi.abstractio.ExpiryDurationPartitionedPaths;

public class TracesDiskPaths extends ExpiryDurationPartitionedPaths<String> {
  public TracesDiskPaths(Path dataDir, long idxExpiryDuration, String baseFileName) {
    super(dataDir, idxExpiryDuration, baseFileName);
  }
}

package org.okapi.traces.query;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TraceFileLocator {
  private final Path baseDir;

  public TraceFileLocator(Path baseDir) { this.baseDir = baseDir; }

  public List<Path> locate(String tenantId, String application, long startMillis, long endMillis) {
    long startBucket = startMillis / 3_600_000L;
    long endBucket = endMillis / 3_600_000L;
    List<Path> files = new ArrayList<>();
    for (long b = startBucket; b <= endBucket; b++) {
      Path p = baseDir.resolve(tenantId).resolve(application).resolve("tracefile." + b + ".bin");
      if (Files.exists(p)) files.add(p);
    }
    return files;
  }
}


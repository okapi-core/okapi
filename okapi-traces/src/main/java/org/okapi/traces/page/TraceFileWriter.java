package org.okapi.traces.page;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TraceFileWriter {
  private final Path baseDir;
  private final Map<String, OutputStream> streams;

  public TraceFileWriter(Path baseDir) {
    this.baseDir = baseDir;
    this.streams = new ConcurrentHashMap<>();
  }

  public void write(String tenantId, String application, SpanPage page) throws IOException {
    long hourBucket = page.getTsStartMillis() / 3_600_000L;
    String key = tenantId + "::" + application + "::" + hourBucket;
    OutputStream os = streams.get(key);
    if (os == null) {
      synchronized (this) {
        os = streams.get(key);
        if (os == null) {
          Path dir = baseDir.resolve(tenantId).resolve(application);
          Files.createDirectories(dir);
          Path file = dir.resolve("tracefile." + hourBucket + ".bin");
          os = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
          streams.put(key, os);
        }
      }
    }
    os.write(page.serialize());
    os.flush();
  }
}


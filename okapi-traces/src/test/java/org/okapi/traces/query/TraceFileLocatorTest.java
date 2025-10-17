package org.okapi.traces.query;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class TraceFileLocatorTest {

  @Test
  void locates_files_across_buckets() throws Exception {
    Path dir = Files.createTempDirectory("okapi-locator");
    try {
      String tenant = "t";
      String app = "a";
      Path base = dir.resolve(tenant).resolve(app);
      Files.createDirectories(base);
      // write buckets 100..102
      for (long hb = 100; hb <= 102; hb++) {
        Files.write(base.resolve("tracefile." + hb + ".bin"), new byte[] {1,2,3});
      }
      TraceFileLocator loc = new TraceFileLocator(dir);
      var files = loc.locate(tenant, app, 100*3_600_000L, 102*3_600_000L);
      assertEquals(3, files.size());
    } finally {
      Files.walk(dir)
          .sorted((a,b) -> b.getNameCount()-a.getNameCount())
          .forEach(p -> { try { Files.deleteIfExists(p);} catch (Exception ignored) {} });
    }
  }
}


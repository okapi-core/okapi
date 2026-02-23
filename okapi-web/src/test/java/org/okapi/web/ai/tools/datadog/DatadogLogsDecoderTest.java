package org.okapi.web.ai.tools.datadog;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

public class DatadogLogsDecoderTest {
  private final Gson gson = new Gson();

  @Test
  void decodesBasicLogs() throws IOException {
    runCase("logs_basic");
  }

  private void runCase(String name) throws IOException {}

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path));
  }
}

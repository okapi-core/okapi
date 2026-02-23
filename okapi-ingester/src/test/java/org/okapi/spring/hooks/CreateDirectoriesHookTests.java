package org.okapi.spring.hooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.okapi.logs.config.LogsCfg;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.traces.config.TracesCfg;

@ExtendWith(MockitoExtension.class)
public class CreateDirectoriesHookTests {

  @TempDir Path tempDir;
  @Mock TracesCfg tracesCfg;
  @Mock LogsCfg logsCfg;
  @Mock MetricsCfg metricsCfg;

  @BeforeEach
  void setup() {
    Mockito.when(tracesCfg.getDataDir()).thenReturn(tempDir.resolve("traces").toString());
    Mockito.when(logsCfg.getDataDir()).thenReturn(tempDir.resolve("logs").toString());
    Mockito.when(metricsCfg.getDataDir()).thenReturn(tempDir.resolve("metrics").toString());
  }

  @Test
  void testCreateAllDirectories() throws IOException {
    var hook = new CreateDirectoriesHook(tracesCfg, logsCfg, metricsCfg);
    hook.run();

    assert (Files.exists(tempDir.resolve("traces")));
    assert (Files.exists(tempDir.resolve("logs")));
    assert (Files.exists(tempDir.resolve("metrics")));
    // should be directories
    assert (Files.isDirectory(tempDir.resolve("traces")));
    assert (Files.isDirectory(tempDir.resolve("logs")));
    assert (Files.isDirectory(tempDir.resolve("metrics")));
  }

  @Test
  void testDoesNotCreateExistingDirectories() throws IOException {
    var hook = new CreateDirectoriesHook(tracesCfg, logsCfg, metricsCfg);
    var tracesPath = tempDir.resolve("traces");
    var logsPath = tempDir.resolve("logs");
    var metricsPath = tempDir.resolve("metrics");
    Files.createDirectories(tracesPath);
    var testFile = Files.write(tracesPath.resolve("test.txt"), "test".getBytes());
    Files.createDirectories(logsPath);
    Files.createDirectories(metricsPath);

    hook.run();
    // existing directory should still exist
    assert (Files.exists(tracesPath));
    // existing file should still exist
    assert (Files.exists(testFile));
    assert (Files.exists(logsPath));
    assert (Files.exists(metricsPath));
  }
}

package org.okapi.spring.hooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.okapi.logs.config.LogsCfg;
import org.okapi.metrics.config.MetricsCfg;
import org.okapi.spring.configs.Profiles;
import org.okapi.traces.config.TracesCfg;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.PROFILE_OKAPI_ENGINE)
public class CreateDirectoriesHook {
  TracesCfg tracesCfg;
  LogsCfg logsCfg;
  MetricsCfg metricsCfg;

  public CreateDirectoriesHook(TracesCfg tracesCfg, LogsCfg logsCfg, MetricsCfg metricsCfg) {
    this.tracesCfg = tracesCfg;
    this.logsCfg = logsCfg;
    this.metricsCfg = metricsCfg;
  }

  public void run() throws IOException {
    createDirectoryIfNotExists(tracesCfg.getDataDir());
    createDirectoryIfNotExists(logsCfg.getDataDir());
    createDirectoryIfNotExists(metricsCfg.getDataDir());
  }

  public void createDirectoryIfNotExists(String path) throws IOException {
    if (!Files.exists(Path.of(path))) {
      Files.createDirectories(Path.of(path));
    }
  }
}

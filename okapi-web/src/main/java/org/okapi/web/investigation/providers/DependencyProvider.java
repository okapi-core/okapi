package org.okapi.web.investigation.providers;

import java.util.List;
import lombok.AllArgsConstructor;
import org.okapi.web.investigation.ctx.finders.*;

@AllArgsConstructor
public class DependencyProvider {
  MetricPathFinder metricPathFinder;
  TracesPathFinder tracesPathFinder;
  LogPathFinder logPathFinder;
  ConfigPathFinder configPathFinder;

  public List<MetricPath> getRelatedMetricPaths(String dependencyName) {
    // Placeholder implementation
    return metricPathFinder.findRelateMetrics(dependencyName);
  }

  public List<LogPath> getRelatedLogPaths(String dependencyName) {
    // Placeholder implementation
    return logPathFinder.findRelatedLogs(dependencyName);
  }

  public List<ConfigPath> getRelatedConfigPaths(String dependencyName) {
    // Placeholder implementation
    return configPathFinder.findRelatedConfigs(dependencyName);
  }

  public List<TracePath> getRelatedTracePaths(String dependencyName) {
    // Placeholder implementation
    return tracesPathFinder.findTracePaths(dependencyName);
  }
}

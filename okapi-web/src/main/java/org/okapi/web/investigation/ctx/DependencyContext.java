/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.investigation.ctx;

import java.io.PrintWriter;
import java.util.List;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.investigation.ctx.finders.*;

@AllArgsConstructor
@Getter
@Builder
public class DependencyContext implements PrintableContext {
  List<MetricPath> metricPaths;
  List<LogPath> logPaths;

  @Override
  public void print(PrintWriter writer) {
    writer.write("<telemetry-context>\n");
    writer.write("<metric-paths>\n");
    for (var path : metricPaths) {
      writer.write(printMetricPath(path) + "\n");
    }
    writer.write("<metric-paths>\n");
    writer.write("<log-paths>\n");
    for (var path : logPaths) {
      writer.write(printLogPath(path) + "\n");
    }
    writer.write("<log-paths>\n");
    writer.write("<telemetry-context>\n");
  }

  public String printLogPath(LogPath path) {
    var sb = new StringBuilder();
    sb.append(path.getPath());
    sb.append(printLabels(path.getTags()));
    return sb.toString();
  }

  public String printTracePath(TracePath path) {
    var sb = new StringBuilder();
    sb.append(path.getPath());
    sb.append(printLabels(path.getTags()));
    return sb.toString();
  }

  public String printMetricPath(MetricPath path) {
    var sb = new StringBuilder();
    sb.append(path.getPath());
    sb.append(printLabels(path.getTags()));
    return sb.toString();
  }

  public String printLabels(TreeMap<String, String> labels) {
    var sb = new StringBuilder();
    sb.append("{");
    var first = true;
    for (var entry : labels.entrySet()) {
      if (!first) {
        sb.append(", ");
      }
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }
}

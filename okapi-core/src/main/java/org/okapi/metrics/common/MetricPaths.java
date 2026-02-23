package org.okapi.metrics.common;

import java.util.Map;
import java.util.TreeMap;
import org.okapi.rest.metrics.query.GetMetricsRequest;

public class MetricPaths {

  public static String convertToUnivPath(GetMetricsRequest request) {
    // separator shouldn't clash with the rest of the system
    return getMetricPath(request.getMetric(), request.getTags());
  }

  public static String getUnivPath(String tenantId, String localPath) {
    return tenantId + ":" + localPath;
  }

  public static String getMetricPath(String universalMetricName, Map<String, String> unsortedTags) {
    var sb = new StringBuilder();
    var sortedMap = new TreeMap<String, String>(unsortedTags);
    sb.append(universalMetricName);
    sb.append("{");
    var first = true;
    for (var entry : sortedMap.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  public static String localPath(String name, Map<String, String> tags) {
    return getMetricPath(name, tags != null ? tags : Map.of());
  }
}

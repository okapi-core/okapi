package org.okapi.metrics.common;

import java.util.Map;
import org.okapi.rest.metrics.query.GetMetricsRequest;

public class MetricPaths {

  public static String convertToUnivPath(GetMetricsRequest request) {
    // separator shouldn't clash with the rest of the system
    return convertToUnivPath(
        request.getTenantId(), request.getMetricName(), request.getTags());
  }

  public static String getUnivPath(String tenantId, String localPath){
    return tenantId + ":" + localPath;
  }

  public static String getMetricPath(String universalMetricName, Map<String, String> tags) {
    var sb = new StringBuilder();
    sb.append(universalMetricName);
    sb.append("{");
    var first = true;
    for (var entry : tags.entrySet()) {
      if (!first) {
        sb.append(",");
      }
      sb.append(entry.getKey()).append("=").append(entry.getValue());
      first = false;
    }
    sb.append("}");
    return sb.toString();
  }

  public static String convertToUnivPath(
      String tenantId, String metricName, Map<String, String> tags) {
    var universalMetricName = tenantId + ":" + metricName;
    return getMetricPath(universalMetricName, tags);
  }

  public static String localPath(String name, Map<String, String> tags) {
    return getMetricPath(name, tags);
  }

  public static String univPath(String tenantId, String name, Map<String, String> tags) {
    return tenantId + ":" + getMetricPath(name, tags);
  }
}

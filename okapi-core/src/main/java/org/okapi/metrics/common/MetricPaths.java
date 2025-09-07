package org.okapi.metrics.common;

import java.util.Map;
import org.okapi.rest.metrics.GetMetricsRequestInternal;
import org.okapi.rest.metrics.SubmitMetricsRequestInternal;

public class MetricPaths {

  public static String convertToPath(SubmitMetricsRequestInternal request) {
    // separator shouldn't clash with the rest of teh system
    return convertToPath(request.getTenantId(), request.getMetricName(), request.getTags());
  }

  public static String convertToPath(GetMetricsRequestInternal request) {
    // separator shouldn't clash with the rest of teh system
    return convertToPath(request.getTenantId(), request.getMetricName(), request.getTags());
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

  public static String convertToPath(String tenantId, String metricName, Map<String, String> tags) {
    var universalMetricName = tenantId + ":" + metricName;
    return getMetricPath(universalMetricName, tags);
  }

  public static String convertToPath(String universalMetricName, Map<String, String> tags) {
    return getMetricPath(universalMetricName, tags);
  }

  public static MetricsContext getMetricsContext(SubmitMetricsRequestInternal request) {
    return new MetricsContext(request.getTags().getOrDefault("trace_id", null));
  }

  public static String localPath(String name, Map<String, String> tags) {
    return getMetricPath(name, tags);
  }
}

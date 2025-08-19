package org.okapi.metrics.common;

import com.okapi.rest.metrics.GetMetricsRequestInternal;
import com.okapi.rest.metrics.SubmitMetricsRequestInternal;
import java.util.Map;

public class MetricPaths {

  public static String convertToPath(SubmitMetricsRequestInternal request) {
    // separator shouldn't clash with the rest of teh system
    return convertToPath(request.getTenantId(), request.getMetricName(), request.getTags());
  }

  public static String convertToPath(GetMetricsRequestInternal request) {
    // separator shouldn't clash with the rest of teh system
    return convertToPath(request.getTenantId(), request.getMetricName(), request.getTags());
  }

  public static String getMetricPath(String name, Map<String, String> tags) {
    var sb = new StringBuilder();
    sb.append(name);
    sb.append("{");
    for (var entry : tags.entrySet()) {
      sb.append(entry.getKey()).append("=").append(entry.getValue());
    }
    sb.append("}");
    return sb.toString();
  }

  public static String convertToPath(String tenantId, String metricName, Map<String, String> tags) {
    var universalMetricName = tenantId + ":" + metricName;
    return getMetricPath(universalMetricName, tags);
  }

  public static MetricsContext getMetricsContext(SubmitMetricsRequestInternal request) {
    return new MetricsContext(request.getTags().getOrDefault("trace_id", null));
  }
}

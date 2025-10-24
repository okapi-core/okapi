package org.okapi.swim.k8s;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "okapi.swim.k8s")
public class K8sDiscoveryProperties {
  // Value for the fixed label key "okapi_service" to select pods (e.g., okapi-logs, okapi-traces)
  private String svcName;

  // Namespace to search in; if null/blank, will derive from serviceaccount namespace
  private String namespace;

  // Include pods not yet Ready; default false
  private boolean includeNotReady;

  // Reconnect/backoff settings (milliseconds)
  private long watchReconnectInitialBackoffMillis = 1000;
  private long watchReconnectMaxBackoffMillis = 15000;
}

package org.okapi.metrics.service;

import org.okapi.rest.metrics.ForwardedMetricsRequest;

public interface MetricsForwarder {
  void forward(String ip, int port, ForwardedMetricsRequest exportMetricsRequest);
}

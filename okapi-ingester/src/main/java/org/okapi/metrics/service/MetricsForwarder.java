/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.service;

import org.okapi.rest.metrics.ForwardedMetricsRequest;

public interface MetricsForwarder {
  void forward(String ip, int port, ForwardedMetricsRequest exportMetricsRequest);
}

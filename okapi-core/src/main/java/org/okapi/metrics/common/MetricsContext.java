/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.common;

import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MetricsContext {
  String contextId;

  public static MetricsContext createContext(String id) {
    return new MetricsContext(id);
  }

  public Optional<String> getContextId() {
    return Optional.ofNullable(contextId);
  }
}

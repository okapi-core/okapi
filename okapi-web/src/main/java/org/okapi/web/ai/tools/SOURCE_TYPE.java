/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import lombok.Getter;

public enum SOURCE_TYPE {
  DATADOG_METRICS("datadog_metrics"),
  DATADOG_LOGS("datadog_logs"),
  DATADOG_TRACES("datadog_traces"),
  LOKI("loki"),
  TEMPO("tempo"),
  MIMIR("mimir"),
  PROMETHEUS("prometheus");

  @Getter String name;

  SOURCE_TYPE(String name) {
    this.name = name;
  }
}

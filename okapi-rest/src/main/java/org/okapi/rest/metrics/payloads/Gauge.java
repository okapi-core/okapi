/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.metrics.payloads;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class Gauge {
  private List<Long> ts;
  private List<Float> value;

  public void lock() {
    ts = Collections.unmodifiableList(ts);
    value = Collections.unmodifiableList(value);
  }
}

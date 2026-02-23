/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.metrics.ch;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class ChGetSumQueryTemplate {
  String table;
  String resource;
  String metric;
  Map<String, String> tags;
  String histoType;
  long ts;
  long te;
}

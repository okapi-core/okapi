/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder
@Accessors(fluent = true)
@AllArgsConstructor
@Getter
public class LogIngestRecord {
  public static final String SVC_GLOBAL = "svc_global";

  long tsMillis;
  String traceId;
  int level;
  String body;
  String service;

  public LogIngestRecord(long tsMillis, String traceId, int level, String body) {
    this.tsMillis = tsMillis;
    this.traceId = traceId;
    this.level = level;
    this.body = body;
    this.service = SVC_GLOBAL;
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.service;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

public interface TraceForwarder {
  void forward(String ip, int port, String app, ExportTraceServiceRequest traceServiceRequest);
}

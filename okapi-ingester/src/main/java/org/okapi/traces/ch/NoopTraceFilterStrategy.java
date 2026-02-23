package org.okapi.traces.ch;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;

public class NoopTraceFilterStrategy implements TraceFilterStrategy {
  @Override
  public boolean shouldPrune(ExportTraceServiceRequest request) {
    return false;
  }
}

package org.okapi.traces.metrics;

public class NoopMetricsEmitter implements MetricsEmitter {
  @Override public void emitPageRead(String tenantId, String application) {}
  @Override public void emitPageTimeSkipped(String tenantId, String application) {}
  @Override public void emitBloomChecked(String tenantId, String application) {}
  @Override public void emitBloomHit(String tenantId, String application) {}
  @Override public void emitBloomMiss(String tenantId, String application) {}
  @Override public void emitPageParsed(String tenantId, String application) {}
  @Override public void emitPageParseError(String tenantId, String application) {}
  @Override public void emitSpansMatched(String tenantId, String application, long count) {}
}


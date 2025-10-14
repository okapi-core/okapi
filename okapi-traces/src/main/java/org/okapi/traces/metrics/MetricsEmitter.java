package org.okapi.traces.metrics;

public interface MetricsEmitter {
  // Page/bloom metrics
  void emitPageRead(String tenantId, String application);
  void emitPageTimeSkipped(String tenantId, String application);
  void emitBloomChecked(String tenantId, String application);
  void emitBloomHit(String tenantId, String application);
  void emitBloomMiss(String tenantId, String application);
  void emitPageParsed(String tenantId, String application);
  void emitPageParseError(String tenantId, String application);
  void emitSpansMatched(String tenantId, String application, long count);

  // Writer metrics
  default void emitTracefileWriteBytes(String tenantId, String application, long hourBucket, long bytes) {}
  default void emitTracefileWriteFailure(String tenantId, String application, long hourBucket) {}
}


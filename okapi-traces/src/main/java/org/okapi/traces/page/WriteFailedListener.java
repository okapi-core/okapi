package org.okapi.traces.page;

public interface WriteFailedListener {
  void onWriteFaile(String tenantId, String application, SpanPage page);
}

package org.okapi.metrics.otel;

public interface RewritePipeline {
  String rewrite(String s);
}

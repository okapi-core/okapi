package org.okapi.metrics.otel;

public class IdentityRewritePipeline implements RewritePipeline {
  @Override
  public String rewrite(String s) {
    return s;
  }
}

package org.okapi.metrics.query;

import org.okapi.protos.metrics.OffsetAndLen;

public interface OffsetFilter {
  boolean shouldRead(OffsetAndLen offsetAndLen);
}

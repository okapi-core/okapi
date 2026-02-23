/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import java.util.Arrays;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.byterange.RangeIterationException;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.traces.io.SpanPageMetadata;

public class SpanFilter implements PageFilter<BinarySpanRecordV2, SpanPageMetadata> {
  byte[] spanId;

  @Override
  public Kind kind() {
    return Kind.SPAN;
  }

  @Override
  public boolean shouldReadPage(SpanPageMetadata pageMeta) throws RangeIterationException {
    return pageMeta.maybeContainsSpanId(spanId);
  }

  @Override
  public boolean matchesRecord(BinarySpanRecordV2 record) {
    return Arrays.equals(record.getSpan().getSpanId().toByteArray(), spanId);
  }
}

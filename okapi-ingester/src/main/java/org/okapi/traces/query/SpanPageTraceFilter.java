package org.okapi.traces.query;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.byterange.RangeIterationException;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.traces.io.SpanPageMetadata;

@Slf4j
@AllArgsConstructor
public class SpanPageTraceFilter implements PageFilter<BinarySpanRecordV2, SpanPageMetadata> {
  byte[] traceId;

  @Override
  public Kind kind() {
    return Kind.TRACE;
  }

  @Override
  public boolean shouldReadPage(SpanPageMetadata pageMeta) throws RangeIterationException {
    return pageMeta.maybeContainsTraceId(traceId);
  }

  @Override
  public boolean matchesRecord(BinarySpanRecordV2 record) {
    var traceId = record.getSpan().getTraceId().toByteArray();
    return traceId != null && Arrays.equals(this.traceId, traceId);
  }
}

package org.okapi.logs.query;

import java.util.Objects;
import lombok.Value;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.byterange.RangeIterationException;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.primitives.BinaryLogRecordV1;

@Value
public class LogPageTraceFilter implements PageFilter<BinaryLogRecordV1, LogPageMetadata> {
  String traceId;

  @Override
  public Kind kind() {
    return Kind.TRACE;
  }

  @Override
  public boolean shouldReadPage(LogPageMetadata pageMeta) throws RangeIterationException {
    return pageMeta.maybeContainsTraceId(traceId);
  }

  @Override
  public boolean matchesRecord(BinaryLogRecordV1 record) {
    return Objects.equals(record.getTraceId(), traceId);
  }
}

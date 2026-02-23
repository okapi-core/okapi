package org.okapi.queryproc;

import java.util.List;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.traces.io.SpanPageMetadata;

public interface TraceQueryProcessor {
  List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg)
      throws Exception;
}

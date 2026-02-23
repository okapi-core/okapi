package org.okapi.queryproc;

import java.util.List;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinaryLogRecordV1;

public interface LogsQueryProcessor {
  List<BinaryLogRecordV1> getLogs(
      String logStream,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg)
      throws Exception;
}

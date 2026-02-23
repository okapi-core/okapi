package org.okapi.logs.query.processor;

import java.util.ArrayList;
import java.util.List;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.intervals.IntervalUtils;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.queryproc.LogsQueryProcessor;

public class BufferPoolLogsQueryProcessor implements LogsQueryProcessor {
  private final LogsBufferPool pool;

  public BufferPoolLogsQueryProcessor(LogsBufferPool pool) {
    this.pool = pool;
  }

  @Override
  public List<BinaryLogRecordV1> getLogs(
      String logStream,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg) {
    var active = pool.snapshotActivePage(LogStreamIdentifier.of(logStream));
    var out = new ArrayList<BinaryLogRecordV1>();
    if (active != null
        && IntervalUtils.isOverlapping(start, end, active.getTsStart(), active.getTsEnd())) {
      out.addAll(filter.getMatchingRecords(active.getLogBodySnapshot().getLogDocs()));
    }
    // Include sealed pages that intersect the query range
    for (var sealed : pool.snapshotSealedPages(LogStreamIdentifier.of(logStream), start, end)) {
      if (IntervalUtils.isOverlapping(start, end, sealed.getTsStart(), sealed.getTsEnd())) {
        out.addAll(filter.getMatchingRecords(sealed.getLogBodySnapshot().getLogDocs()));
      }
    }
    return out;
  }
}

package org.okapi.logs.query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.okapi.logs.config.LogsConfigProperties;
import org.okapi.logs.index.PageIndex;
import org.okapi.logs.index.PageIndexEntry;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.stereotype.Service;

@Service
public class QueryService {
  private final LogsConfigProperties cfg;
  private final LogPageBufferPool pool;
  private final LogFileWriter writer;

  public QueryService(LogsConfigProperties cfg, LogPageBufferPool pool) {
    this.cfg = cfg;
    this.pool = pool;
    this.writer = new LogFileWriter(this.cfg);
  }

  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter)
      throws IOException {
    List<LogPayloadProto> out = new ArrayList<>();

    // Active page snapshot (if any) - quick path
    LogPage active = pool.snapshotActivePage(tenantId, logStream);
    if (active != null && rangesOverlap(active.getTsStart(), active.getTsEnd(), start, end)) {
      out.addAll(FilterEvaluator.apply(active, filter));
    }

    // On-disk pages via index
    Path part = writer.partitionDir(tenantId, logStream);
    Path idx = part.resolve("logfile.idx");
    Path bin = part.resolve("logfile.bin");
    PageIndex pageIndex = new PageIndex(idx);
    for (PageIndexEntry e : pageIndex.range(start, end)) {
      byte[] bytes = writer.readRange(bin, e.getOffset(), e.getLength());
      LogPage page = LogPageSerializer.deserialize(bytes);
      out.addAll(FilterEvaluator.apply(page, filter));
    }
    return out;
  }

  private boolean rangesOverlap(long aStart, long aEnd, long bStart, long bEnd) {
    return !(aEnd < bStart || aStart > bEnd);
  }
}

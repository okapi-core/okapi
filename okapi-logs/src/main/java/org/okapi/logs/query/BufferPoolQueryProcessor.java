package org.okapi.logs.query;

import java.util.ArrayList;
import java.util.List;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.runtime.LogPageBufferPool;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.stereotype.Service;

@Service
public class BufferPoolQueryProcessor implements QueryProcessor {
  private final LogPageBufferPool pool;

  public BufferPoolQueryProcessor(LogPageBufferPool pool) {
    this.pool = pool;
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter) {
    List<LogPayloadProto> out = new ArrayList<>();
    LogPage active = pool.snapshotActivePage(tenantId, logStream);
    if (active == null) return out;
    if (active.getTsEnd() < start || active.getTsStart() > end) return out;
    out.addAll(FilterEvaluator.apply(active, filter));
    return out;
  }
}

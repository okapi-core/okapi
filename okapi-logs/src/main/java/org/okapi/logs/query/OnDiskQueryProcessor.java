package org.okapi.logs.query;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.okapi.logs.index.PageIndex;
import org.okapi.logs.index.PageIndexEntry;
import org.okapi.logs.io.LogFileWriter;
import org.okapi.logs.io.LogPage;
import org.okapi.logs.io.LogPageSerializer;
import org.okapi.protos.logs.LogPayloadProto;
import org.springframework.stereotype.Service;

@Service
public class OnDiskQueryProcessor implements QueryProcessor {
  private final LogFileWriter writer;

  public OnDiskQueryProcessor(org.okapi.logs.config.LogsConfigProperties cfg) {
    this.writer = new LogFileWriter(cfg);
  }

  @Override
  public List<LogPayloadProto> getLogs(
      String tenantId, String logStream, long start, long end, LogFilter filter) throws IOException {
    List<LogPayloadProto> out = new ArrayList<>();
    long cur = floorToHour(start);
    long endHour = floorToHour(end);
    while (cur <= endHour) {
      Path part = writer.partitionDir(tenantId, logStream, cur);
      Path idx = part.resolve("logfile.idx");
      Path bin = part.resolve("logfile.bin");
      PageIndex pageIndex = new PageIndex(idx);
      for (PageIndexEntry e : pageIndex.range(start, end)) {
        byte[] bytes = writer.readRange(bin, e.getOffset(), e.getLength());
        LogPage page = LogPageSerializer.deserialize(bytes);
        out.addAll(FilterEvaluator.apply(page, filter));
      }
      cur += 3600_000L;
    }
    return out;
  }

  private long floorToHour(long tsMs) {
    java.time.ZonedDateTime z = java.time.Instant.ofEpochMilli(tsMs).atZone(java.time.ZoneId.of("UTC"));
    java.time.ZonedDateTime f = z.withMinute(0).withSecond(0).withNano(0);
    return f.toInstant().toEpochMilli();
  }
}

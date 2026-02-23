package org.okapi.logs.query.processor;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.logs.io.LogPageMetadata;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.queryproc.LogsQueryProcessor;
import org.okapi.queryproc.MultisourceDocumentJoiner;
import org.okapi.spring.configs.properties.QueryCfg;

@Slf4j
public class MultiSourceLogsQueryProcessor implements LogsQueryProcessor {
  private final LogsQueryProcessor buffer;
  private final LogsQueryProcessor disk;
  private final LogsQueryProcessor s3;
  private final LogsQueryProcessor memberSet;
  private final ExecutorService exec;

  public MultiSourceLogsQueryProcessor(
      BufferPoolLogsQueryProcessor buffer,
      OnDiskLogsQueryProcessor disk,
      S3LogsQueryProcessor s3,
      PeersLogsQueryProcessor peersQp,
      QueryCfg cfg) {
    this.buffer = buffer;
    this.disk = disk;
    this.s3 = s3;
    this.memberSet = peersQp;
    this.exec = Executors.newFixedThreadPool(cfg.getLogsQueryProcPoolSize());
  }

  @Override
  public List<BinaryLogRecordV1> getLogs(
      String logStream,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg)
      throws IOException {
    var joiner =
        new MultisourceDocumentJoiner<BinaryLogRecordV1>(
            Arrays.asList(
                () ->
                    cfg.bufferPool
                        ? buffer.getLogs(logStream, start, end, filter, cfg)
                        : Collections.emptyList(),
                () ->
                    cfg.disk
                        ? disk.getLogs(logStream, start, end, filter, cfg)
                        : Collections.emptyList(),
                () ->
                    cfg.s3
                        ? s3.getLogs(logStream, start, end, filter, cfg)
                        : Collections.emptyList(),
                () ->
                    cfg.fanOut
                        ? memberSet.getLogs(logStream, start, end, filter, cfg)
                        : Collections.emptyList()),
            exec);
    var out = joiner.getJoinedStream(Duration.of(10, ChronoUnit.SECONDS));
    out.sort(Comparator.comparingLong(BinaryLogRecordV1::getTsMillis));
    return out;
  }
}

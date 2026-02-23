package org.okapi.logs.query.processor;

import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.byterange.DiskByteRangeSupplier;
import org.okapi.byterange.LengthPrefixPageAndMdIterator;
import org.okapi.logs.io.*;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.logs.query.PageMetadataIteratorQp;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.queryproc.LogsQueryProcessor;
import org.okapi.spring.configs.Qualifiers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

@Slf4j
public class OnDiskLogsQueryProcessor implements LogsQueryProcessor {
  private final LogPageNonChecksummedCodec logPageCodec = new LogPageNonChecksummedCodec();
  private final LogsDiskPaths diskLogBinPaths;
  private final long maxQueryRange;

  public OnDiskLogsQueryProcessor(
      LogsDiskPaths diskLogBinPaths,
      @Autowired @Qualifier(Qualifiers.DISK_QP_MAX_QUERY_WIN) Duration maxQueryWindow) {
    this.diskLogBinPaths = diskLogBinPaths;
    this.maxQueryRange = maxQueryWindow.toMillis();
  }


  public void checkQueryWindow(long start, long end) {
    if (end - start > maxQueryRange) {
      throw new IllegalArgumentException(
          "Query time range exceeds maximum allowed of " + maxQueryRange + " ms");
    }
  }

  ///  todo: fixme - should query shards not logStream
  @Override
  public List<BinaryLogRecordV1> getLogs(
      String logStream,
      long start,
      long end,
      PageFilter<BinaryLogRecordV1, LogPageMetadata> filter,
      QueryConfig cfg)
      throws Exception {
    checkQueryWindow(start, end);
    var matching = new ArrayList<BinaryLogRecordV1>();
    for (var path :
        diskLogBinPaths.listLogBinFiles(LogStreamIdentifier.of(logStream), start, end)) {
      if (!Files.exists(path)) {
        continue;
      }
      var byteSupplier = new DiskByteRangeSupplier(path);
      var iterator = new LengthPrefixPageAndMdIterator(byteSupplier);
      var qp = new PageMetadataIteratorQp<>(iterator, filter, logPageCodec);
      matching.addAll(qp.getMatchingRecords());
      byteSupplier.close();
    }
    return matching;
  }
}

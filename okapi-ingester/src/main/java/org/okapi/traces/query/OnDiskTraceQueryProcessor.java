package org.okapi.traces.query;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.LogStreamIdentifier;
import org.okapi.byterange.DiskByteRangeSupplier;
import org.okapi.byterange.LengthPrefixPageAndMdIterator;
import org.okapi.logs.query.PageMetadataIteratorQp;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.queryproc.TraceQueryProcessor;
import org.okapi.traces.io.SpanPageCodec;
import org.okapi.traces.io.SpanPageMetadata;
import org.okapi.traces.paths.TracesDiskPaths;

@Slf4j
public class OnDiskTraceQueryProcessor implements TraceQueryProcessor {

  private final SpanPageCodec spanPageCodec = new SpanPageCodec();
  private final TracesDiskPaths diskLogBinPaths;

  public OnDiskTraceQueryProcessor(TracesDiskPaths diskLogBinPaths) {
    this.diskLogBinPaths = diskLogBinPaths;
  }

  ///  todo: fixme - need to query shards not `app` streams.
  @Override
  public List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg)
      throws Exception {
    var matching = new ArrayList<BinarySpanRecordV2>();
    for (var path : diskLogBinPaths.listLogBinFiles(LogStreamIdentifier.of(app), start, end)) {
      log.info("Querying traces from file: {}", path);
      if (!Files.exists(path)) {
        log.info("File doesn't exist: {}", path);
        continue;
      }
      log.info("Started trace querying from file: {}", path);
      var byteSupplier = new DiskByteRangeSupplier(path);
      var iterator = new LengthPrefixPageAndMdIterator(byteSupplier);
      var qp = new PageMetadataIteratorQp<>(iterator, filter, spanPageCodec);
      matching.addAll(qp.getMatchingRecords());
      byteSupplier.close();
    }
    return matching;
  }
}

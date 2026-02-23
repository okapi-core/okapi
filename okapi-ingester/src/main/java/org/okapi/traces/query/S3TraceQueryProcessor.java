/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.abstractio.BinFilesPrefixRegistry;
import org.okapi.abstractio.PartNames;
import org.okapi.byterange.LengthPrefixPageAndMdIterator;
import org.okapi.byterange.RangeIterationException;
import org.okapi.byterange.S3ByteRangeSupplier;
import org.okapi.io.NotEnoughBytesException;
import org.okapi.io.StreamReadingException;
import org.okapi.logs.query.PageMetadataIteratorQp;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.queryproc.TraceQueryProcessor;
import org.okapi.s3.S3ByteRangeCache;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.SpanPageCodec;
import org.okapi.traces.io.SpanPageMetadata;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class S3TraceQueryProcessor implements TraceQueryProcessor {

  private final TracesCfg cfg;
  private final S3Client s3Client;
  private final S3ByteRangeCache s3ByteRangeCache;
  private final BinFilesPrefixRegistry prefixRegistry;
  private final SpanPageCodec spanPageCodec = new SpanPageCodec();

  public S3TraceQueryProcessor(
      TracesCfg cfg,
      S3Client s3Client,
      S3ByteRangeCache s3ByteRangeCache,
      BinFilesPrefixRegistry prefixRegistry) {
    this.cfg = cfg;
    this.s3Client = s3Client;
    this.s3ByteRangeCache = s3ByteRangeCache;
    this.prefixRegistry = prefixRegistry;
  }

  @Override
  public List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg) {
    if (!cfg.s3) return List.of();
    var prefixes = getMatchingPartitions(app, start, end);
    return prefixes.stream()
        .parallel()
        .map(
            prefix -> {
              try {
                var records = getLogsFromPrefix(this.cfg.getS3Bucket(), prefix, filter);
                log.info("Fetched {} records from prefix: {}", records.size(), prefix);
                return records;
              } catch (IOException
                  | StreamReadingException
                  | RangeIterationException
                  | NotEnoughBytesException e) {
                log.error("Error querying logs from prefix: " + prefix, e);
                return Collections.<BinarySpanRecordV2>emptyList();
              }
            })
        .flatMap(List::stream)
        .toList();
  }

  public List<BinarySpanRecordV2> getLogsFromPrefix(
      String bucket, String prefix, PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter)
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var s3ByteRangeSupplier = new S3ByteRangeSupplier(bucket, prefix, s3Client, s3ByteRangeCache);
    var iterator = new LengthPrefixPageAndMdIterator(s3ByteRangeSupplier);
    var qp = new PageMetadataIteratorQp<>(iterator, filter, spanPageCodec);
    return new ArrayList<>(qp.getMatchingRecords());
  }

  public List<String> getMatchingPartitions(String logStream, long start, long end) {
    var matchingPrefixes = new ArrayList<String>();
    for (long ts = start; ts <= end; ts += cfg.getIdxExpiryDuration()) {
      var part = ts / cfg.getIdxExpiryDuration();
      var prefixes =
          prefixRegistry.getAllPrefixesForLogBinFile(
              cfg.getS3Bucket(), cfg.getS3BasePrefix(), logStream, PartNames.SPAN_FILE_PART, part);
      matchingPrefixes.addAll(prefixes);
    }
    return matchingPrefixes;
  }
}

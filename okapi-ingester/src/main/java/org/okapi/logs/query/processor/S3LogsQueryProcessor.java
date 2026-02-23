/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.query.processor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.LogPageNonChecksummedCodec;
import org.okapi.logs.query.PageMetadataIteratorQp;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinaryLogRecordV1;
import org.okapi.queryproc.LogsQueryProcessor;
import org.okapi.s3.S3ByteRangeCache;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
public class S3LogsQueryProcessor implements LogsQueryProcessor {
  // todo: return statistics on bytes scanned, total
  private final LogsCfg cfg;
  private final S3Client s3Client;
  private final S3ByteRangeCache s3ByteRangeCache;
  private final BinFilesPrefixRegistry prefixRegistry;
  private final LogPageNonChecksummedCodec logPageCodec = new LogPageNonChecksummedCodec();

  public S3LogsQueryProcessor(
      LogsCfg cfg,
      S3Client s3Client,
      S3ByteRangeCache s3ByteRangeCache,
      BinFilesPrefixRegistry prefixRegistry) {
    this.cfg = cfg;
    this.s3Client = s3Client;
    this.s3ByteRangeCache = s3ByteRangeCache;
    this.prefixRegistry = prefixRegistry;
  }

  @Override
  public List<BinaryLogRecordV1> getLogs(
      String logStream, long start, long end, PageFilter filter, QueryConfig qcfg)
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    if (!qcfg.s3) return List.of();
    var prefixes = getMatchingPartitions(logStream, start, end);
    return prefixes.stream()
        .parallel()
        .map(
            prefix -> {
              try {
                return getLogsFromPrefix(cfg.getS3Bucket(), prefix, filter);
              } catch (IOException
                  | StreamReadingException
                  | RangeIterationException
                  | NotEnoughBytesException e) {
                log.error("Error querying logs from prefix: " + prefix, e);
                return new ArrayList<BinaryLogRecordV1>();
              }
            })
        .flatMap(List::stream)
        .sorted(Comparator.comparingLong(BinaryLogRecordV1::getTsMillis))
        .toList();
  }

  public List<BinaryLogRecordV1> getLogsFromPrefix(String bucket, String prefix, PageFilter filter)
      throws IOException, StreamReadingException, RangeIterationException, NotEnoughBytesException {
    var s3ByteRangeSupplier = new S3ByteRangeSupplier(bucket, prefix, s3Client, s3ByteRangeCache);
    var iterator = new LengthPrefixPageAndMdIterator(s3ByteRangeSupplier);
    var qp = new PageMetadataIteratorQp<>(iterator, filter, logPageCodec);
    return new ArrayList<>(qp.getMatchingRecords());
  }

  public List<String> getMatchingPartitions(String logStream, long start, long end) {
    var matchingPrefixes = new ArrayList<String>();
    for (long ts = start; ts <= end; ts += cfg.getIdxExpiryDuration()) {
      var part = ts / cfg.getIdxExpiryDuration();
      var prefixes =
          prefixRegistry.getAllPrefixesForLogBinFile(
              cfg.getS3Bucket(), cfg.getS3BasePrefix(), logStream, PartNames.LOG_FILE_PART, part);
      matchingPrefixes.addAll(prefixes);
    }
    return matchingPrefixes;
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces.query;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractfilter.PageFilter;
import org.okapi.logs.query.QueryConfig;
import org.okapi.primitives.BinarySpanRecordV2;
import org.okapi.queryproc.MultisourceDocumentJoiner;
import org.okapi.queryproc.TraceQueryProcessor;
import org.okapi.spring.configs.properties.QueryCfg;
import org.okapi.traces.io.SpanPageMetadata;

@Slf4j
public class MultiSourceTraceQueryProcessor implements TraceQueryProcessor {

  SpanBufferPoolQueryProcessor buffer;
  S3TraceQueryProcessor s3;
  PeersTraceQueryProcessor memberSet;
  OnDiskTraceQueryProcessor disk;
  ExecutorService executorService;

  public MultiSourceTraceQueryProcessor(
      SpanBufferPoolQueryProcessor buffer,
      S3TraceQueryProcessor s3,
      PeersTraceQueryProcessor memberSet,
      OnDiskTraceQueryProcessor disk,
      QueryCfg cfg) {
    this.buffer = buffer;
    this.disk = disk;
    this.s3 = s3;
    this.memberSet = memberSet;
    this.executorService = Executors.newFixedThreadPool(cfg.getLogsQueryProcPoolSize());
  }

  @Override
  public List<BinarySpanRecordV2> getTraces(
      String app,
      long start,
      long end,
      PageFilter<BinarySpanRecordV2, SpanPageMetadata> filter,
      QueryConfig cfg)
      throws Exception {

    var joiner =
        new MultisourceDocumentJoiner<BinarySpanRecordV2>(
            Arrays.asList(
                () ->
                    cfg.bufferPool
                        ? buffer.getTraces(app, start, end, filter, cfg)
                        : Collections.emptyList(),
                () ->
                    cfg.disk
                        ? disk.getTraces(app, start, end, filter, cfg)
                        : Collections.emptyList(),
                () -> cfg.s3 ? s3.getTraces(app, start, end, filter, cfg) : Collections.emptyList(),
                () ->
                    cfg.fanOut
                        ? memberSet.getTraces(app, start, end, filter, cfg)
                        : Collections.emptyList()),
            executorService);
    var out = joiner.getJoinedStream(Duration.of(10, ChronoUnit.SECONDS));
    out.sort(Comparator.comparingLong(r -> r.getSpan().getStartTimeUnixNano()));
    return out;
  }
}

/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.traces;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Function;
import org.okapi.abstractbufferpool.BUFFER_POOL_TYPE;
import org.okapi.abstractbufferpool.PeriodicallyFlushedBufPool;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;
import org.okapi.traces.config.TracesCfg;
import org.okapi.traces.io.*;
import org.okapi.traces.paths.TracesDiskPaths;
import org.okapi.wal.lsn.Lsn;

public class TracesBufferPool
    extends PeriodicallyFlushedBufPool<
        SpanIngestionRecord,
        SpanPage,
        SpanIngestionRecord,
        SpanPageSnapshot,
        SpanPageMetadata,
        SpanPageBody,
        String> {

  public TracesBufferPool(
      TracesCfg cfg,
      Function<StreamIdentifier<String>, SpanPage> pageFactory,
      Codec<SpanPage, SpanPageSnapshot, SpanPageMetadata, SpanPageBody> codec,
      TracesDiskPaths diskLogBinPaths,
      MeterRegistry meterRegistry,
      WalResourcesPerStream<String> walResourcesPerStream) {
    super(
        BUFFER_POOL_TYPE.LOGS,
        pageFactory,
        codec,
        diskLogBinPaths,
        meterRegistry,
        cfg.getSealedPageCap(),
        cfg.getSealedPageTtlMs(),
        walResourcesPerStream);
  }

  @Override
  public void consume(
      Lsn lsn, StreamIdentifier<String> identifier, SpanIngestionRecord ingestionRecord) {
    append(lsn, identifier, ingestionRecord);
  }
}

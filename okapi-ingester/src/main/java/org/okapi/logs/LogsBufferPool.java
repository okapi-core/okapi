package org.okapi.logs;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.okapi.abstractbufferpool.BUFFER_POOL_TYPE;
import org.okapi.abstractbufferpool.PeriodicallyFlushedBufPool;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.*;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

@Slf4j
public class LogsBufferPool
    extends PeriodicallyFlushedBufPool<
        LogIngestRecord,
        LogPage,
        LogIngestRecord,
        LogPageSnapshot,
        LogPageMetadata,
        LogPageBody,
        String> {
  public LogsBufferPool(
      LogsCfg cfg,
      Function<StreamIdentifier<String>, LogPage> pageFactory,
      Codec<LogPage, LogPageSnapshot, LogPageMetadata, LogPageBody> codec,
      LogsDiskPaths diskLogBinPaths,
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
  public void consume(Lsn lsn, StreamIdentifier<String> streamIdentifier, LogIngestRecord record) {
    append(lsn, streamIdentifier, record);
  }
}

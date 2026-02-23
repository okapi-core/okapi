/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.testmodules;

import com.google.inject.Inject;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.okapi.abstractio.DiskLogBinPaths;
import org.okapi.abstractio.WalResourcesPerStream;
import org.okapi.logs.LogsBufferPool;
import org.okapi.logs.config.LogsCfg;
import org.okapi.logs.io.*;
import org.okapi.logs.paths.LogsDiskPaths;
import org.okapi.pages.Codec;
import org.okapi.streams.StreamIdentifier;
import org.okapi.wal.lsn.Lsn;

public class LoggingLogsBufferPool extends LogsBufferPool {
  public record ConsumeCommand(Lsn lsn, String streamId, LogIngestRecord record) {}

  @Getter private final List<ConsumeCommand> commands = new ArrayList<>();

  @Inject
  public LoggingLogsBufferPool(
      LogsCfg logsCfg,
      DiskLogBinPaths<String> diskLogBinPaths,
      MeterRegistry meterRegistry,
      WalResourcesPerStream<String> walResourcesPerStream) {
    super(
        logsCfg,
        streamId ->
            new LogPage(
                logsCfg.getExpectedInsertions(),
                logsCfg.getMaxPageWindowMs(),
                logsCfg.getMaxPageBytes()),
        (Codec<LogPage, LogPageSnapshot, LogPageMetadata, LogPageBody>)
            new LogPageNonChecksummedCodec(),
        (LogsDiskPaths) diskLogBinPaths,
        meterRegistry,
        walResourcesPerStream);
  }

  @Override
  public void consume(Lsn lsn, StreamIdentifier<String> streamIdentifier, LogIngestRecord record) {
    commands.add(new ConsumeCommand(lsn, streamIdentifier.getStreamId(), record));
    super.consume(lsn, streamIdentifier, record);
  }
}
